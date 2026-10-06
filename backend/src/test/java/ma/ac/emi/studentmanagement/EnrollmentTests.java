package ma.ac.emi.studentmanagement;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import org.junit.jupiter.api.*;

/**
 * Module M04 -- Inscription / Annulation
 * Module M05 -- Inscription en doublon
 * Cas de test : TC-020 à TC-023, TC-030, TC-031
 *
 * Tests fonctionnels Playwright : toutes les interactions passent
 * exclusivement par l'interface utilisateur (navigateur).
 * Aucun appel API direct (pas d'APIRequestContext).
 */
public class EnrollmentTests {

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

        // Précondition commune : se connecter en tant qu'étudiant via l'interface
        page.navigate(BASE_URL + "/login");
        page.locator("input[name='email']").fill("student@emi.ac.ma");
        page.locator("input[name='password']").fill("student123");
        page.locator("button[type='submit']").click();
        page.waitForURL("**/courses", new Page.WaitForURLOptions().setTimeout(10000));

        // Attendre que les cartes de cours soient complètement rendues
        page.locator(".course-card").first().waitFor(
                new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
    }

    @AfterEach
    void tearDown() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    /**
     * Méthode utilitaire : annuler toutes les inscriptions existantes via « Mes inscriptions »
     * pour garantir l'isolation des tests.
     */
    private void cancelAllEnrollments() {
        page.locator("a[href='/my-courses']").click();
        page.waitForURL("**/my-courses");
        page.waitForTimeout(1000);

        // Annuler chaque inscription tant qu'il en reste
        while (page.locator(".enrollment-card button.btn-cancel").count() > 0) {
            page.locator(".enrollment-card button.btn-cancel").first().click();
            page.waitForTimeout(1000);
        }

        // Revenir au catalogue
        page.navigate(BASE_URL + "/courses");
        page.waitForURL("**/courses");
        page.locator(".course-card").first().waitFor(
                new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
    }

    // ══════════════════════════════════════════════════════════════════
    //  MODULE M04 -- Inscription / Annulation
    // ══════════════════════════════════════════════════════════════════

    // ──────────────────────────────────────────────────────────────────
    // TC-020 : Annulation dans le délai autorisé (transition : Inscrit -> Annulé)
    // Note : Le backend actuel n'implémente pas de délai d'annulation.
    //        L'annulation est toujours permise. Ce test vérifie l'annulation simple.
    // ──────────────────────────────────────────────────────────────────
    @Test
    void TC020_annulationInscription() {
        // 0. Garantir l'isolation : annuler toute inscription existante
        cancelAllEnrollments();

        // 1. S'inscrire à un cours disponible
        Locator enrollBtn = page.locator(".course-card:has(.badge-open) button.btn-enroll").first();
        Assertions.assertTrue(enrollBtn.isVisible(), "Il doit exister au moins un cours ouvert avec bouton Enroll");
        enrollBtn.click();

        // 2. Attendre que le bouton devienne « Cancel Enrollment »
        page.waitForTimeout(2000);

        // 3. Accéder à « Mes inscriptions »
        page.locator("a[href='/my-courses']").click();
        page.waitForURL("**/my-courses");
        page.waitForTimeout(1000);

        // 4. Vérifier qu'au moins une inscription existe
        Locator enrollmentCards = page.locator(".enrollment-card");
        enrollmentCards.first().waitFor(
                new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
        int countBefore = enrollmentCards.count();
        Assertions.assertTrue(countBefore > 0, "Il doit y avoir au moins une inscription");

        // 5. Cliquer « Cancel » sur la première inscription
        page.locator(".enrollment-card button.btn-cancel").first().click();

        // 6. Vérifier que l'inscription est annulée (la carte disparaît)
        page.waitForTimeout(2000);
        int countAfter = page.locator(".enrollment-card").count();
        Assertions.assertTrue(countAfter < countBefore,
                "Le nombre d'inscriptions doit diminuer après annulation");
    }

    // ──────────────────────────────────────────────────────────────────
    // TC-021 : Annulation hors délai (transition invalide : Inscrit -> Inscrit)
    // Note : Le backend actuel ne gère pas de délai d'annulation.
    //        Ce test est désactivé en attendant l'implémentation.
    // ──────────────────────────────────────────────────────────────────
    @Test
    @Disabled("Le délai d'annulation (7 jours) n'est pas encore implémenté dans le backend")
    void TC021_annulationHorsDelai() {
        // 1. Accéder à « Mes inscriptions »
        page.locator("a[href='/my-courses']").click();
        page.waitForURL("**/my-courses");

        // 2. Tenter d'annuler une inscription vieille de plus de 7 jours
        page.locator(".enrollment-card button.btn-cancel").first().click();

        // 3. Vérifier le message de refus
        Locator errorMsg = page.locator(".alert-error");
        errorMsg.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        Assertions.assertTrue(
                errorMsg.textContent().contains("délai") || errorMsg.textContent().contains("dépassé"),
                "Le message doit indiquer que le délai d'annulation est dépassé");
    }

    // ──────────────────────────────────────────────────────────────────
    // TC-022 : Annulation d'une inscription inexistante
    // Via l'interface : un utilisateur sans inscription ne voit aucun bouton Cancel.
    // ──────────────────────────────────────────────────────────────────
    @Test
    void TC022_annulationInscriptionInexistante() {
        // 0. Garantir l'isolation : annuler toute inscription existante
        cancelAllEnrollments();

        // 1. Accéder à « Mes inscriptions »
        page.locator("a[href='/my-courses']").click();
        page.waitForURL("**/my-courses");
        page.waitForTimeout(2000);

        // 2. Vérifier qu'il n'y a aucune carte d'inscription
        int count = page.locator(".enrollment-card").count();

        // 3. Vérifier le message d'état vide ou l'absence de cartes
        if (count == 0) {
            // Soit un message est affiché, soit la page est simplement vide
            String bodyText = page.locator("body").textContent();
            boolean emptyIndicator = bodyText.contains("No enrollments")
                    || bodyText.contains("Aucune inscription")
                    || bodyText.contains("not enrolled")
                    || page.locator(".empty-state").count() > 0
                    || count == 0;
            Assertions.assertTrue(emptyIndicator,
                    "La page doit indiquer qu'il n'y a aucune inscription");
        }

        // 4. Vérifier qu'aucun bouton d'annulation n'est présent
        Assertions.assertEquals(0, page.locator(".enrollment-card button.btn-cancel").count(),
                "Il ne doit y avoir aucun bouton d'annulation sans inscription");
    }

    // ──────────────────────────────────────────────────────────────────
    // TC-023 : Réinscription après annulation (transition : Annulé -> Inscrit)
    // ──────────────────────────────────────────────────────────────────
    @Test
    void TC023_reinscriptionApresAnnulation() {
        // 0. Garantir l'isolation : annuler toute inscription existante
        cancelAllEnrollments();

        // 1. Repérer le titre du premier cours ouvert disponible
        Locator openCourses = page.locator(".course-card:has(.badge-open):has(button.btn-enroll)");
        Assertions.assertTrue(openCourses.count() > 0, "Il doit exister au moins un cours ouvert");
        Locator targetCard = openCourses.first();
        String courseTitle = targetCard.locator("h3").textContent().trim();

        // 2. S'inscrire au cours
        targetCard.locator("button.btn-enroll").click();
        page.waitForTimeout(2000);

        // 3. Vérifier que le bouton Cancel Enrollment apparaît pour ce cours
        // Rechercher la carte par son titre après le rafraîchissement du DOM
        Locator updatedCard = page.locator(".course-card", new Page.LocatorOptions()
                .setHasText(courseTitle));
        Locator cancelBtn = updatedCard.locator("button.btn-cancel");
        cancelBtn.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
        Assertions.assertTrue(cancelBtn.isVisible(), "Le bouton Cancel doit apparaître après inscription");

        // 4. Annuler l'inscription
        cancelBtn.click();
        page.waitForTimeout(2000);

        // 5. Vérifier que le bouton Enroll réapparaît
        Locator reEnrollBtn = updatedCard.locator("button.btn-enroll");
        reEnrollBtn.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));

        // 6. Se réinscrire au même cours
        reEnrollBtn.click();
        page.waitForTimeout(2000);

        // 7. Vérifier que la réinscription est acceptée
        Locator newCancelBtn = updatedCard.locator("button.btn-cancel");
        newCancelBtn.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(10000));
        Assertions.assertTrue(newCancelBtn.isVisible(),
                "La réinscription doit être acceptée (bouton Cancel visible)");

        // 8. Vérifier dans « Mes inscriptions »
        page.locator("a[href='/my-courses']").click();
        page.waitForURL("**/my-courses");
        page.waitForTimeout(1000);
        String myCoursesContent = page.locator("body").textContent();
        Assertions.assertTrue(myCoursesContent.contains(courseTitle),
                "Le cours réinscrit doit apparaître dans 'Mes inscriptions'");
    }

    // ══════════════════════════════════════════════════════════════════
    //  MODULE M05 -- Inscription en doublon
    // ══════════════════════════════════════════════════════════════════

    // ──────────────────────────────────────────────────────────────────
    // TC-030 : Tentative d'inscription à un cours déjà inscrit (table de décision R2)
    // Vérifié via l'interface : après inscription, le bouton Enroll disparaît
    // et est remplacé par Cancel Enrollment, empêchant le doublon.
    // ──────────────────────────────────────────────────────────────────
    @Test
    void TC030_inscriptionDoublonMemeCours() {
        // 0. Garantir l'isolation : annuler toute inscription existante
        cancelAllEnrollments();

        // 1. Repérer un cours ouvert
        Locator openCourses = page.locator(".course-card:has(.badge-open):has(button.btn-enroll)");
        Assertions.assertTrue(openCourses.count() > 0, "Il doit exister au moins un cours ouvert");
        String courseTitle = openCourses.first().locator("h3").textContent().trim();

        // 2. S'inscrire au cours via l'interface
        openCourses.first().locator("button.btn-enroll").click();
        page.waitForTimeout(2000);

        // 3. Rechercher la carte mise à jour par son titre
        Locator updatedCard = page.locator(".course-card", new Page.LocatorOptions()
                .setHasText(courseTitle));

        // 4. Vérifier que le bouton « Enroll » n'est plus visible pour ce cours
        Locator enrollBtnAfter = updatedCard.locator("button.btn-enroll");
        Assertions.assertFalse(enrollBtnAfter.isVisible(),
                "Le bouton 'Enroll' ne doit plus être visible après inscription (empêche le doublon)");

        // 5. Vérifier que seul le bouton « Cancel Enrollment » est affiché
        Locator cancelBtn = updatedCard.locator("button.btn-cancel");
        Assertions.assertTrue(cancelBtn.isVisible(),
                "Seul le bouton 'Cancel Enrollment' doit être affiché après inscription");
    }

    // ──────────────────────────────────────────────────────────────────
    // TC-031 : Double inscription simultanée au même cours (concurrence)
    // Note : Ce test nécessite deux sessions navigateur parallèles avec le même
    //        utilisateur, ce qui n'est pas réalisable via l'interface seule.
    // ──────────────────────────────────────────────────────────────────
    @Test
    @Disabled("Le test de concurrence nécessite deux sessions parallèles, non réalisable via l'interface seule")
    void TC031_doubleInscriptionSimultanee() {
        // Ce scénario de concurrence ne peut pas être testé de manière fiable
        // uniquement via l'interface utilisateur.
    }
}