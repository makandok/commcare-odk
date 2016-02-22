package org.commcare.android.tests.formnav;

import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageButton;

import org.commcare.android.CommCareTestRunner;
import org.commcare.android.database.SqlStorage;
import org.commcare.android.database.user.models.FormRecord;
import org.commcare.android.mocks.FormAndDataSyncerFake;
import org.commcare.android.models.AndroidSessionWrapper;
import org.commcare.android.util.TestAppInstaller;
import org.commcare.android.util.TestUtils;
import org.commcare.dalvik.BuildConfig;
import org.commcare.dalvik.R;
import org.commcare.dalvik.activities.CommCareHomeActivity;
import org.commcare.dalvik.application.CommCareApplication;
import org.commcare.session.CommCareSession;
import org.commcare.session.SessionNavigator;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.reference.ResourceReferenceFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.activities.FormEntryActivity;
import org.odk.collect.android.views.QuestionsView;
import org.odk.collect.android.widgets.IntegerWidget;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowEnvironment;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * @author Phillip Mates (pmates@dimagi.com).
 */
@Config(application = CommCareApplication.class,
        constants = BuildConfig.class)
@RunWith(CommCareTestRunner.class)
public class EndOfFormTest {

    private static final String TAG = EndOfFormTest.class.getSimpleName();

    @Before
    public void setup() {
        // needed to resolve "jr://resource" type references
        ReferenceManager._().addReferenceFactory(new ResourceReferenceFactory());

        TestUtils.initializeStaticTestStorage();
        TestAppInstaller.setupPrototypeFactory();

        TestAppInstaller appTestInstaller =
                new TestAppInstaller("jr://resource/commcare-apps/form_nav_tests/profile.ccpr",
                        "test", "123");
        appTestInstaller.installAppAndLogin();
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);
    }

    /**
     * Test filling out and saving a form ending in a hidden repeat group. This
     * type of form exercises uncommon end-of-form code paths
     */
    @Test
    public void testHiddenRepeatAtEndOfForm() {
        CommCareHomeActivity homeActivity = buildHomeActivityForFormEntryLaunch();

        ShadowActivity shadowActivity = Shadows.shadowOf(homeActivity);
        Intent formEntryIntent = shadowActivity.getNextStartedActivity();

        // make sure the form entry activity should be launched
        String intentActivityName = formEntryIntent.getComponent().getClassName();
        assertTrue(intentActivityName.equals(FormEntryActivity.class.getName()));

        ShadowActivity shadowFormEntryActivity = navigateFormEntry(formEntryIntent);

        // trigger CommCareHomeActivity.onActivityResult for the completion of
        // FormEntryActivity
        shadowActivity.receiveResult(formEntryIntent,
                shadowFormEntryActivity.getResultCode(),
                shadowFormEntryActivity.getResultIntent());
        assertStoredForms();
    }

    private CommCareHomeActivity buildHomeActivityForFormEntryLaunch() {
        AndroidSessionWrapper sessionWrapper =
                CommCareApplication._().getCurrentSessionWrapper();
        CommCareSession session = sessionWrapper.getSession();
        session.setCommand("m0-f0");

        CommCareHomeActivity homeActivity =
                Robolectric.buildActivity(CommCareHomeActivity.class).create().get();
        // make sure we don't actually submit forms by using a fake form submitter
        homeActivity.setFormAndDataSyncer(new FormAndDataSyncerFake(homeActivity));
        SessionNavigator sessionNavigator = homeActivity.getSessionNavigator();
        sessionNavigator.startNextSessionStep();
        return homeActivity;
    }

    private ShadowActivity navigateFormEntry(Intent formEntryIntent) {
        // launch form entry
        FormEntryActivity formEntryActivity =
                Robolectric.buildActivity(FormEntryActivity.class).withIntent(formEntryIntent)
                        .create().start().resume().get();

        ImageButton nextButton = (ImageButton)formEntryActivity.findViewById(R.id.nav_btn_next);

        // enter an answer for the question
        QuestionsView questionsView = formEntryActivity.getODKView();
        IntegerWidget favoriteNumber = (IntegerWidget)questionsView.getWidgets().get(0);
        favoriteNumber.setAnswer("2");
        assertTrue(nextButton.getTag().equals(FormEntryActivity.NAV_STATE_NEXT));
        // Finish off the form even by clicking next.
        // The form progress meter thinks there is more to do, but that is a bug.
        nextButton.performClick();

        ShadowActivity shadowFormEntryActivity = Shadows.shadowOf(formEntryActivity);
        while (!shadowFormEntryActivity.isFinishing()) {
            Log.d(TAG, "Waiting for the form to save and the form entry activity to finish");
        }

        return shadowFormEntryActivity;
    }

    private void assertStoredForms() {
        SqlStorage<FormRecord> formsStorage =
                CommCareApplication._().getUserStorage(FormRecord.class);

        int unsentForms = formsStorage.getIDsForValue(FormRecord.META_STATUS,
                FormRecord.STATUS_UNSENT).size();
        int incompleteForms = formsStorage.getIDsForValue(FormRecord.META_STATUS,
                FormRecord.STATUS_INCOMPLETE).size();
        assertEquals("There should be a single form waiting to be sent", 1, unsentForms);
        assertEquals("There shouldn't be any forms saved as incomplete", 0, incompleteForms);
    }
}
