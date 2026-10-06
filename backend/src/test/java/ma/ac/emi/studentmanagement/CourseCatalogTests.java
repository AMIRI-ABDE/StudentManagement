package ma.ac.emi.studentmanagement;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import org.junit.jupiter.api.*;

/**
 * Module M02 -- Catalogue de cours
 * Cas de test : TC-040 à TC-042
 *
 * Note : L'application actuelle affiche tous les cours sans filtre de recherche
 * par spécialité. Les tests TC-040 et TC-041 documentent le comportement attendu
 * pour une future implémentation de la recherche. TC-042 est adapté à l'interface
 * existante (liste de cours avec détails visibles).
 */
public class CourseCatalogTests {

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

        // Précondition commune : se connecter en tant qu'étudiant
        page.navigate(BASE_URL + "/login");
        page.locator("input[name='email']").fill("student@emi.ac.ma");
        page.locator("input[name='password']").fill("student123");
        page.locator("button[type='submit']").click();
        page.waitForURL("**/courses");
    }

    @AfterEach
    void tearDown() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    // ──────────────────────────────────────────────────────────────────
    // TC-040 : Recherche multicritère par spécialité
    // Note : La fonctionnalité de recherche/filtre n'est pas encore implémentée.
    // ──────────────────────────────────────────────────────────────────
    @Test
    @Disabled("La recherche multicritère par spécialité n'est pas encore implémentée")
    void TC040_rechercheMulticritereParSpecialite() {
        // 1. Sélectionner le filtre spécialité = « Cloud »
        page.locator("select[name='speciality']").selectOption("Cloud");

        // 2. Lancer la recherche
        page.locator("button.btn-search").click();

        // 3. Vérifier que seuls les cours Cloud sont affichés
        Locator courseCards = page.locator(".course-card");
        int count = courseCards.count();
        Assertions.assertEquals(5, count, "5 cours en spécialité Cloud doivent être affichés");

        // 4. Vérifier que chaque cours appartient à la spécialité Cloud
        for (int i = 0; i < count; i++) {
            String text = courseCards.nth(i).textContent();
            Assertions.assertTrue(text.contains("Cloud"),
                    "Chaque cours affiché doit appartenir à la spécialité Cloud");
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // TC-041 : Recherche avec zéro résultat
    // Note : La fonctionnalité de recherche n'est pas encore implémentée.
    // ──────────────────────────────────────────────────────────────────
    @Test
    @Disabled("La recherche n'est pas encore implémentée")
    void TC041_rechercheAvecZeroResultat() {
        // 1. Saisir « Robotique » dans la barre de recherche
        page.locator("input[name='search']").fill("Robotique");

        // 2. Soumettre la recherche
        page.locator("input[name='search']").press("Enter");

        // 3. Vérifier que la liste est vide avec un message approprié
        Assertions.assertTrue(
                page.locator("body").textContent().contains("Aucun cours") ||
                        page.locator("body").textContent().contains("No courses"),
                "Un message 'Aucun cours trouvé' doit être affiché"
        );
    }

    // ──────────────────────────────────────────────────────────────────
    // TC-042 : Consultation des détails d'un cours
    // Adapté : dans l'interface actuelle, les détails (titre, capacité,
    // nombre de places) sont visibles directement sur les cartes du catalogue.
    // ──────────────────────────────────────────────────────────────────
    @Test
    void TC042_consultationDetailsCours() {
        // 1. Vérifier que le catalogue de cours est affiché
        Locator courseCards = page.locator(".course-card");
        courseCards.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        Assertions.assertTrue(courseCards.count() > 0, "Le catalogue doit contenir au moins un cours");

        // 2. Vérifier qu'un cours spécifique est affiché (ex: « Mathematics » chargé par DataLoader)
        String pageContent = page.locator("body").textContent();
        Assertions.assertTrue(pageContent.contains("Mathematics"),
                "Le cours 'Mathematics' doit être visible dans le catalogue");

        // 3. Vérifier que les détails du cours sont visibles : capacité et nombre d'inscrits
        Locator firstCard = courseCards.first();
        String cardContent = firstCard.textContent();
        Assertions.assertTrue(cardContent.contains("/"),
                "La capacité (format inscrits/max) doit être affichée");

        // 4. Vérifier la présence du badge de statut (Open ou Full)
        Locator badge = firstCard.locator(".badge");
        Assertions.assertTrue(badge.isVisible(), "Un badge de statut doit être affiché");
    }
}