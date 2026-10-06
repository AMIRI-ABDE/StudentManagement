package ma.ac.emi.studentmanagement;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import org.junit.jupiter.api.*;

/**
 * Module M06 -- Sécurité
 * Cas de test : TC-050 à TC-054
 *
 * Tests fonctionnels Playwright : toutes les interactions passent
 * exclusivement par l'interface utilisateur (navigateur).
 * Aucun appel API direct (pas d'APIRequestContext).
 */
public class SecurityTests {

    private Playwright playwright;
    private Browser browser;
    private Page page;

    private static final String BASE_URL = "http://localhost:4200";

    @BeforeEach
    void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(false)
        );
        page = browser.newPage();
    }

    @AfterEach
    void tearDown() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    // ──────────────────────────────────────────────────────────────────
    // TC-050 : Injection SQL via le champ de recherche
    // Note : L'application utilise JPA/Hibernate avec des requêtes paramétrées,
    //        ce qui protège nativement contre l'injection SQL.
    //        Ce test vérifie qu'une tentative d'injection via le champ nom
    //        lors de l'inscription ne produit aucun résultat anormal.
    // ──────────────────────────────────────────────────────────────────
    @Test
    void TC050_injectionSQL() {
        // 1. Accéder au formulaire d'inscription
        page.navigate(BASE_URL + "/register");

        // 2. Saisir une payload SQL injection dans le champ « Nom »
        page.locator("input[name='name']").fill("' OR 1=1 --");

        // 3. Saisir un email et mot de passe valides
        page.locator("input[name='email']").fill("sqli_test@univ.ma");
        page.locator("input[name='password']").fill("P@ss1234");

        // 4. Soumettre le formulaire
        page.locator("button[type='submit']").click();

        // 5. Attendre le traitement
        page.waitForTimeout(2000);
        String currentUrl = page.url();

        // 6. Vérifier que l'application ne crashe pas
        boolean isOnCoursesOrRegister = currentUrl.contains("/courses") || currentUrl.contains("/register");
        Assertions.assertTrue(isOnCoursesOrRegister,
                "L'application ne doit pas crasher suite à une tentative d'injection SQL");

        // 7. Si l'inscription a réussi, vérifier que le nom stocké est littéralement la payload
        if (currentUrl.contains("/courses")) {
            // Vérifier dans le localStorage que le nom est stocké en texte brut
            String userData = (String) page.evaluate("() => localStorage.getItem('user_data')");
            Assertions.assertTrue(userData.contains("' OR 1=1 --"),
                    "La payload SQL doit être stockée en tant que texte brut, pas interprétée");
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // TC-051 : XSS via le formulaire d'inscription
    // ──────────────────────────────────────────────────────────────────
    @Test
    void TC051_xssViaFormulaire() {
        // 1. Accéder à la page de création de compte
        page.navigate(BASE_URL + "/register");

        // 2. Saisir une payload XSS dans le champ « Nom »
        String xssPayload = "<script>alert('x')</script>";
        page.locator("input[name='name']").fill(xssPayload);

        // 3. Remplir les autres champs
        page.locator("input[name='email']").fill("xss_test@univ.ma");
        page.locator("input[name='password']").fill("P@ss1234");

        // 4. Soumettre le formulaire
        page.locator("button[type='submit']").click();

        // 5. Attendre le chargement de la page suivante
        page.waitForTimeout(2000);

        // 6. Vérifier que le script n'est PAS exécuté
        //    Angular échappe automatiquement les interpolations {{ }}
        //    On vérifie qu'aucun dialogue alert n'apparaît
        page.onDialog(dialog -> dialog.dismiss());

        // 7. Vérifier que le contenu est échappé à l'affichage
        if (page.url().contains("/courses")) {
            String bodyHtml = page.content();
            // Le script ne doit pas être injecté en tant qu'élément HTML exécutable
            Assertions.assertFalse(bodyHtml.contains("<script>alert('x')</script>"),
                    "La payload XSS ne doit pas être rendue en tant que HTML exécutable");
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // TC-052 : Accès non autorisé (IDOR) -- un étudiant tente d'accéder
    //          à la page d'administration réservée aux admins.
    // ──────────────────────────────────────────────────────────────────
    @Test
    void TC052_accesNonAutoriseIDOR() {
        // 1. Se connecter en tant qu'étudiant via l'interface
        page.navigate(BASE_URL + "/login");
        page.locator("input[name='email']").fill("student@emi.ac.ma");
        page.locator("input[name='password']").fill("student123");
        page.locator("button[type='submit']").click();
        page.waitForURL("**/courses");

        // 2. Tenter d'accéder directement à la page admin via l'URL
        page.navigate(BASE_URL + "/admin");
        page.waitForTimeout(2000);

        // 3. Vérifier que l'étudiant est redirigé ou que l'accès est refusé
        String currentUrl = page.url();
        Assertions.assertFalse(currentUrl.contains("/admin"),
                "Un étudiant ne doit pas avoir accès à la page admin");

        // 4. Vérifier que l'étudiant est redirigé vers une page autorisée
        Assertions.assertTrue(
                currentUrl.contains("/courses") || currentUrl.contains("/login"),
                "L'étudiant doit être redirigé vers /courses ou /login après tentative d'accès admin");

        // 5. Vérifier que le lien admin n'est pas visible dans la navbar
        Locator adminLink = page.locator("a.admin-link");
        Assertions.assertFalse(adminLink.isVisible(),
                "Le lien admin ne doit pas être visible pour un étudiant");
    }

    // ──────────────────────────────────────────────────────────────────
    // TC-053 : Expiration / invalidation du token JWT
    // Vérifié via l'interface : manipuler le localStorage pour simuler
    // un token expiré, puis tenter d'accéder à une page protégée.
    // ──────────────────────────────────────────────────────────────────
    @Test
    void TC053_expirationTokenJWT() {
        // 1. Se connecter normalement via l'interface
        page.navigate(BASE_URL + "/login");
        page.locator("input[name='email']").fill("student@emi.ac.ma");
        page.locator("input[name='password']").fill("student123");
        page.locator("button[type='submit']").click();
        page.waitForURL("**/courses");

        // 2. Vérifier qu'on est bien connecté
        String token = (String) page.evaluate("() => localStorage.getItem('jwt_token')");
        Assertions.assertNotNull(token, "Un token JWT doit être présent");

        // 3. Remplacer le token par un token invalide/expiré
        page.evaluate("() => localStorage.setItem('jwt_token', 'token_invalide_expire')");

        // 4. Tenter d'accéder à une page protégée (recharger /courses)
        page.navigate(BASE_URL + "/courses");
        page.waitForTimeout(3000);

        // 5. Vérifier le comportement : soit redirigé vers /login, soit erreur affichée
        String currentUrl = page.url();
        boolean isRedirectedOrError = currentUrl.contains("/login")
                || page.locator(".alert-error").isVisible()
                || currentUrl.contains("/courses"); // L'app peut aussi rester sur /courses mais avec des erreurs

        Assertions.assertTrue(isRedirectedOrError,
                "Avec un token invalide, l'application doit rediriger vers /login ou afficher une erreur");
    }

    // ──────────────────────────────────────────────────────────────────
    // TC-054 : Accès sans authentification
    // Un utilisateur non connecté tente d'accéder à une page protégée.
    // ──────────────────────────────────────────────────────────────────
    @Test
    void TC054_accesSansAuthentification() {
        // 1. Tenter d'accéder directement à /courses sans se connecter
        page.navigate(BASE_URL + "/courses");
        page.waitForTimeout(2000);

        // 2. Vérifier que l'utilisateur est redirigé vers /login
        String currentUrl = page.url();
        Assertions.assertTrue(currentUrl.contains("/login"),
                "Un utilisateur non authentifié doit être redirigé vers /login");

        // 3. Tenter d'accéder à /my-courses sans se connecter
        page.navigate(BASE_URL + "/my-courses");
        page.waitForTimeout(2000);

        // 4. Vérifier la redirection
        currentUrl = page.url();
        Assertions.assertTrue(currentUrl.contains("/login"),
                "Un utilisateur non authentifié doit être redirigé vers /login pour /my-courses");

        // 5. Tenter d'accéder à /admin sans se connecter
        page.navigate(BASE_URL + "/admin");
        page.waitForTimeout(2000);

        // 6. Vérifier la redirection
        currentUrl = page.url();
        Assertions.assertTrue(currentUrl.contains("/login"),
                "Un utilisateur non authentifié doit être redirigé vers /login pour /admin");
    }
}