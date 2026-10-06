package ma.ac.emi.studentmanagement;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import org.junit.jupiter.api.*;

/**
 * Module M01 -- Gestion des comptes
 * Cas de test : TC-001 à TC-006
 *
 * Tests fonctionnels Playwright : toutes les interactions passent
 * exclusivement par l'interface utilisateur (navigateur).
 * Aucun appel API direct (pas d'APIRequestContext).
 */
public class AccountTests {

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
    // TC-001 : Création de compte avec données valides (EP : classe valide)
    // ──────────────────────────────────────────────────────────────────
    @Test
    void TC001_creationCompteAvecDonneesValides() {
        // 1. Accéder à la page d'inscription
        page.navigate(BASE_URL + "/register");

        // 2. Remplir le formulaire avec des données valides (email unique par exécution)
        String uniqueEmail = "alami.sara.tc001." + System.currentTimeMillis() + "@univ.ma";
        page.locator("input[name='name']").fill("Alami Sara");
        page.locator("input[name='email']").fill(uniqueEmail);
        page.locator("input[name='password']").fill("P@ss1234");

        // 3. Soumettre le formulaire
        page.locator("button[type='submit']").click();

        // 4. Vérifier la redirection vers /courses (inscription réussie)
        page.waitForURL("**/courses", new Page.WaitForURLOptions().setTimeout(10000));
        Assertions.assertTrue(page.url().contains("/courses"),
                "L'inscription réussie doit rediriger vers /courses");

        // 5. Vérifier qu'un token JWT est stocké dans le localStorage
        String token = (String) page.evaluate("() => localStorage.getItem('jwt_token')");
        Assertions.assertNotNull(token, "Un token JWT doit être stocké dans le localStorage");
        Assertions.assertFalse(token.isEmpty(), "Le token JWT ne doit pas être vide");

        // 6. Vérifier que les données utilisateur sont stockées
        String userData = (String) page.evaluate("() => localStorage.getItem('user_data')");
        Assertions.assertNotNull(userData, "Les données utilisateur doivent être stockées");
        Assertions.assertTrue(userData.contains(uniqueEmail),
                "L'email doit être présent dans les données utilisateur");
        Assertions.assertTrue(userData.contains("STUDENT"),
                "Le rôle par défaut doit être STUDENT");
    }

    // ──────────────────────────────────────────────────────────────────
    // TC-002 : Création avec email déjà existant (EP : classe invalide)
    // ──────────────────────────────────────────────────────────────────
    @Test
    void TC002_creationAvecEmailDejaExistant() {
        // 1. Précondition : créer un premier compte (email unique par exécution)
        String uniqueEmail = "alami.sara.tc002." + System.currentTimeMillis() + "@univ.ma";
        page.navigate(BASE_URL + "/register");
        page.locator("input[name='name']").fill("Premier Utilisateur");
        page.locator("input[name='email']").fill(uniqueEmail);
        page.locator("input[name='password']").fill("P@ss1234");
        page.locator("button[type='submit']").click();
        page.waitForURL("**/courses", new Page.WaitForURLOptions().setTimeout(10000));

        // 2. Se déconnecter
        page.locator("button.btn-logout").click();
        page.waitForURL("**/login");

        // 3. Tenter une deuxième inscription avec le même email
        page.navigate(BASE_URL + "/register");
        page.locator("input[name='name']").fill("Deuxième Utilisateur");
        page.locator("input[name='email']").fill(uniqueEmail);
        page.locator("input[name='password']").fill("P@ss1234");
        page.locator("button[type='submit']").click();

        // 4. Vérifier qu'un message d'erreur est affiché
        Locator errorMsg = page.locator(".alert-error");
        errorMsg.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
        Assertions.assertTrue(errorMsg.isVisible(),
                "Un message d'erreur doit être affiché pour un email déjà utilisé");

        // 5. Vérifier que le message mentionne l'email
        String errorText = errorMsg.textContent().toLowerCase();
        Assertions.assertTrue(errorText.contains("email") || errorText.contains("already"),
                "Le message d'erreur doit mentionner l'email déjà utilisé");

        // 6. Vérifier qu'on reste sur la page d'inscription
        Assertions.assertTrue(page.url().contains("/register"),
                "L'utilisateur doit rester sur la page d'inscription");
    }

    // ──────────────────────────────────────────────────────────────────
    // TC-003 : Création avec champs obligatoires manquants (EP : classe invalide)
    // ──────────────────────────────────────────────────────────────────
    @Test
    void TC003_creationAvecChampsObligatoiresManquants() {
        // 1. Accéder à la page d'inscription
        page.navigate(BASE_URL + "/register");

        // 2. Remplir uniquement le nom et le mot de passe (email manquant)
        page.locator("input[name='name']").fill("Test User");
        page.locator("input[name='password']").fill("P@ss1234");
        // NE PAS remplir l'email

        // 3. Soumettre le formulaire
        page.locator("button[type='submit']").click();

        // 4. Attendre un court instant
        page.waitForTimeout(2000);

        // 5. Vérifier qu'on reste sur la page d'inscription (pas de redirection)
        Assertions.assertTrue(page.url().contains("/register"),
                "L'inscription sans email doit laisser l'utilisateur sur /register");
    }

    // ──────────────────────────────────────────────────────────────────
    // TC-004 : Connexion avec identifiants corrects
    // ──────────────────────────────────────────────────────────────────
    @Test
    void TC004_connexionAvecIdentifiantsCorrects() {
        // 1. Accéder à la page de connexion
        page.navigate(BASE_URL + "/login");

        // 2. Se connecter avec le compte pré-chargé par DataLoader
        page.locator("input[name='email']").fill("student@emi.ac.ma");
        page.locator("input[name='password']").fill("student123");
        page.locator("button[type='submit']").click();

        // 3. Vérifier la redirection vers /courses
        page.waitForURL("**/courses", new Page.WaitForURLOptions().setTimeout(10000));
        Assertions.assertTrue(page.url().contains("/courses"),
                "La connexion réussie doit rediriger vers /courses");

        // 4. Vérifier qu'un token JWT est stocké dans le localStorage
        String token = (String) page.evaluate("() => localStorage.getItem('jwt_token')");
        Assertions.assertNotNull(token, "Un token JWT doit être stocké après connexion");
        Assertions.assertFalse(token.isEmpty(), "Le token JWT ne doit pas être vide");

        // 5. Vérifier les données utilisateur dans le localStorage
        String userData = (String) page.evaluate("() => localStorage.getItem('user_data')");
        Assertions.assertNotNull(userData, "Les données utilisateur doivent être stockées");
        Assertions.assertTrue(userData.contains("student@emi.ac.ma"),
                "L'email doit être présent dans les données utilisateur");
        Assertions.assertTrue(userData.contains("Demo Student"),
                "Le nom doit être présent dans les données utilisateur");
        Assertions.assertTrue(userData.contains("STUDENT"),
                "Le rôle doit être STUDENT");
    }

    // ──────────────────────────────────────────────────────────────────
    // TC-005 : Connexion avec mot de passe incorrect (EP : invalide)
    // ──────────────────────────────────────────────────────────────────
    @Test
    void TC005_connexionAvecMotDePasseIncorrect() {
        // 1. Accéder à la page de connexion
        page.navigate(BASE_URL + "/login");

        // 2. Saisir un email valide avec un mauvais mot de passe
        page.locator("input[name='email']").fill("student@emi.ac.ma");
        page.locator("input[name='password']").fill("mauvais_mot_de_passe");
        page.locator("button[type='submit']").click();

        // 3. Vérifier qu'un message d'erreur est affiché
        Locator errorMsg = page.locator(".alert-error");
        errorMsg.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
        Assertions.assertTrue(errorMsg.isVisible(),
                "Un message d'erreur doit être affiché pour un mot de passe incorrect");

        // 4. Vérifier le contenu du message
        String errorText = errorMsg.textContent().toLowerCase();
        Assertions.assertTrue(
                errorText.contains("invalid") || errorText.contains("incorrect") || errorText.contains("credentials"),
                "Le message doit indiquer des identifiants invalides");

        // 5. Vérifier qu'on reste sur la page de connexion
        Assertions.assertTrue(page.url().contains("/login"),
                "L'utilisateur doit rester sur la page de connexion");
    }

    // ──────────────────────────────────────────────────────────────────
    // TC-006 : Compte verrouillé après 5 tentatives échouées (BVA : seuil = 5)
    // ──────────────────────────────────────────────────────────────────
    @Test
    @Disabled("Le verrouillage de compte après N tentatives n'est pas encore implémenté dans le backend")
    void TC006_compteVerrouilleApres5TentativesEchouees() {
        // 1. Accéder à la page de connexion
        page.navigate(BASE_URL + "/login");

        // 2. Envoyer 5 tentatives avec un mauvais mot de passe
        for (int i = 1; i <= 5; i++) {
            page.locator("input[name='email']").fill("student@emi.ac.ma");
            page.locator("input[name='password']").fill("mauvais_" + i);
            page.locator("button[type='submit']").click();
            page.waitForTimeout(1000);
        }

        // 3. Tenter une 6ème connexion avec le BON mot de passe
        page.locator("input[name='email']").fill("student@emi.ac.ma");
        page.locator("input[name='password']").fill("student123");
        page.locator("button[type='submit']").click();
        page.waitForTimeout(2000);

        // 4. Vérifier que le compte est verrouillé (pas de redirection vers /courses)
        Assertions.assertTrue(page.url().contains("/login"),
                "Le compte doit être verrouillé après 5 tentatives échouées");

        // 5. Vérifier le message d'erreur
        Locator errorMsg = page.locator(".alert-error");
        Assertions.assertTrue(errorMsg.isVisible(),
                "Un message d'erreur doit indiquer le verrouillage du compte");
    }
}