package com.mf;

import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import com.hp.lft.sdk.*;
import com.hp.lft.sdk.mobile.*;
import com.mf.utils.*;
import unittesting.*;

public class LeanFtTest extends UnitTestClassBase {
    private boolean noProblem;
    private Device device;
    private static appModel appModel;
    private MobileLabUtils utils = new MobileLabUtils();
    private String userName = "Shahar";
    private String userPassword  = "460d4691b2f164b933e1476fa1";
    private int counter = 0;
    private String osType = "IOS";
    private String osVersion = ">=11.3.0";
    private String osModel = "iPhone";

    @BeforeClass
    public void beforeClass() throws Exception {
    }

    @AfterClass
    public void afterClass() throws Exception {
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        Logging.logMessage("Enter setUp() method ", Logging.LOG_LEVEL.INFO );
        utils.setAppIdentifier("com.mf.iShopping");
        utils.setAppVersion("1.1.5");
        utils.setPackaged(true);
        utils.setInstallApp(true);
        utils.setUninstallApp(true);
        utils.setHighlight(false);

        String appVersion = System.getProperty("appVersion");
        if (appVersion != null) utils.setAppVersion(appVersion);

        String appIdentifier = System.getProperty("appIdentifier");
        if (appIdentifier != null) utils.setAppIdentifier(appIdentifier);

        noProblem = true;

        try {

            initLabResources();

        } catch (Exception ex) {
            Logging.logMessage ("Exception in setup(): " + ex.getMessage(), Logging.LOG_LEVEL.ERROR);
            noProblem = false;
        }
    }

    @AfterMethod
    public void afterMethod() throws Exception {
    }

    /*
    This method runs recursively (calling to itself) to run N cycles of login,
    purchase and logout.
    in this case N=2, to change the number of iterations, modify this line
        if (counter >= 2)
    For instance, to run once -> if (counter >= 1)
    */
    @Test //(threadPoolSize = 10, invocationCount = 2)
    public void test() throws GeneralLeanFtException, InterruptedException {
        if (!noProblem) {
            Assert.fail();
            return;
        }
        counter++;

        try {
            if (utils.isInstallApp())
                postInstallActions();

            Logging.logMessage ("Tap 'Open Menu'", Logging.LOG_LEVEL.INFO);
            openMenu();

            // if user logged in, sign him out
            checkIfLoggedIn();

            // do sign in
            signIn();

            // Run the main test
            runMainTest();

            // do sign out after completing the test
            openMenu();
            signOut();

            if (counter >= 2)
                return;

            utils.setInstallApp(false);
            test();

            if (utils.isUninstallApp()) {
                Logging.logMessage("Un-installing app: " + utils.getApp().getName(), Logging.LOG_LEVEL.INFO);
                utils.getApp().uninstall();
                utils.setInstallApp(false);
            }
            Logging.logMessage ("********** Test completed successfully **********", Logging.LOG_LEVEL.INFO);

        } catch (ReplayObjectNotFoundException ronfex) {
            Logging.logMessage ("error code: " + ronfex.getErrorCode() + " - " + ronfex.getMessage(), Logging.LOG_LEVEL.ERROR);
            Assert.fail();
        }
    }

    private void signOut() throws GeneralLeanFtException {
        Logging.logMessage("Signing out", Logging.LOG_LEVEL.INFO);
        if (utils.isHighlight())
            appModel.AdvantageShoppingApplication().SIGNOUTLabel().highlight();
        appModel.AdvantageShoppingApplication().SIGNOUTLabel().tap();

        if (utils.isHighlight())
            appModel.AdvantageShoppingApplication().SignOutYesButton().highlight();
        appModel.AdvantageShoppingApplication().SignOutYesButton().tap();
    }

    private void signIn() throws GeneralLeanFtException, InterruptedException {
        int appVersion = Integer.parseInt(utils.getAppVersion().replace(".", ""));

        // AOS v1.1.5 was the first version fingerprint authentication was introduced
        if (appVersion < 115)
            signInWithCredentials();
        else {
            /*
                If the app was installed for **this** test execution:
                First, login with credentials
                Next, we need to enable the Biometric login
                Lastly, navigate to the HOME page
            */
            if (utils.isInstallApp()) {
                signInWithCredentials();
                utils.windowSync(5000);

                Logging.logMessage ("Accept the 'fingerprint authentication' option", Logging.LOG_LEVEL.INFO);
                if (utils.isHighlight())
                    appModel.AdvantageShoppingApplication().BiometricYESButton().highlight();
                appModel.AdvantageShoppingApplication().BiometricYESButton().tap();

                // Must authenticate with fingerprint to activate the feature for future usage
                Logging.logMessage ("Do fingerprint authentication after credentials - to activate the feature", Logging.LOG_LEVEL.INFO);
                signInWithFingerPrintAuthentication();
                utils.windowSync(2500);

                openMenu();
                if (utils.isHighlight())
                    appModel.AdvantageShoppingApplication().HOMELabel().highlight();
                appModel.AdvantageShoppingApplication().HOMELabel().tap();
            }
            /* if the app was installed before then just sign in with Fingerprint */
            else {
                //enableFingerPrintAuthentication();
                navigateToLogin();
                signInWithFingerPrintAuthentication();
                utils.windowSync(2500);

            }
        }
    }

    /*
    Sign in using user credentials
    */
    private void signInWithCredentials() throws GeneralLeanFtException {
        Logging.logMessage ("Tap login label (credentials)", Logging.LOG_LEVEL.INFO);
        navigateToLogin();

        Logging.logMessage ("Type name", Logging.LOG_LEVEL.INFO);
        if (utils.isHighlight())
            appModel.AdvantageShoppingApplication().UserNameField().highlight();
        appModel.AdvantageShoppingApplication().UserNameField().setText(userName);

        Logging.logMessage ("Type password", Logging.LOG_LEVEL.INFO);
        if (utils.isHighlight())
            appModel.AdvantageShoppingApplication().PasswordField().highlight();
        appModel.AdvantageShoppingApplication().PasswordField().setSecure(userPassword);

        Logging.logMessage ("Tap login button", Logging.LOG_LEVEL.INFO);
        if (utils.isHighlight())
            appModel.AdvantageShoppingApplication().LOGINButton().highlight();
        appModel.AdvantageShoppingApplication().LOGINButton().tap();
    }

    private void signInWithFingerPrintAuthentication() throws GeneralLeanFtException {
        Logging.logMessage("Do fingerprint authentication", Logging.LOG_LEVEL.INFO);
        utils.getApp().simulateAuthentication().succeed();
    }

    /*
    SimulateAuthFailReason.FINGERPRINT_INCOMPLETE // only supported in Android
    SimulateAuthFailReason.SENSOR_DIRTY // only supported in Android
    SimulateAuthFailReason.NOT_RECOGNIZED
    SimulateAuthFailReason.NOT_REGISTERED // only supported in iOS
    SimulateAuthFailReason.LOCKOUT
    */
    private void signInWithFingerPrintAuthenticationFail(SimulateAuthFailReason reason) throws GeneralLeanFtException {
        utils.getApp().simulateAuthentication().fail(reason);
    }

    private void signInWithFingerPrintAuthenticationFail(String reason) throws GeneralLeanFtException {
        utils.getApp().simulateAuthentication().fail(reason);
    }

    /*
    SimulateAuthCancelOrigin.SYSTEM
    SimulateAuthCancelOrigin.USER
    */
    private void signInWithFingerPrintAuthenticationCancel(SimulateAuthCancelOrigin origin) throws GeneralLeanFtException {
        utils.getApp().simulateAuthentication().cancel(origin);
    }

    private void signInWithFingerPrintAuthenticationCancel(String origin) throws GeneralLeanFtException {
        utils.getApp().simulateAuthentication().cancel(origin);
    }

    /*
    Enable the Fingerprint option:
    Set the Toggle
    Check if the 'need to login before activating...' message
        If there, sign in with credentials
        Accept the biometric login message
    */
    private void enableFingerPrintAuthentication() throws GeneralLeanFtException, InterruptedException {
        Logging.logMessage("Navigate to Settings", Logging.LOG_LEVEL.INFO);
        if (utils.isHighlight())
            appModel.AdvantageShoppingApplication().SETTINGSLabel().highlight();
        appModel.AdvantageShoppingApplication().SETTINGSLabel().tap();

        Logging.logMessage("Toggle...", Logging.LOG_LEVEL.INFO);
        if (utils.isHighlight())
            appModel.AdvantageShoppingApplication().LoginUsingFingerprintToggle().highlight();
        appModel.AdvantageShoppingApplication().LoginUsingFingerprintToggle().set(true);

        /*
        Logging.logMessage("Label message: " + appModel.AdvantageShoppingApplication().CredentialsFirstLabel().getText(), Logging.LOG_LEVEL.INFO);
        if (appModel.AdvantageShoppingApplication().CredentialsFirstLabelOkButton().exists(2)) {
            appModel.AdvantageShoppingApplication().CredentialsFirstLabelOkButton().tap();
            openMenu();
            signInWithCredentials();
            utils.windowSync(3000);
            appModel.AdvantageShoppingApplication().BiometricYESButton().tap();
        }
        else // if the message above doesn't exist, we need to re-set the toggle
            appModel.AdvantageShoppingApplication().LoginUsingFingerprintToggle().set(true);
         */
    }

    private void openMenu() throws GeneralLeanFtException{
        Logging.logMessage("Open menu", Logging.LOG_LEVEL.INFO);
        if (utils.isHighlight())
            appModel.AdvantageShoppingApplication().MenuButton().highlight();
        appModel.AdvantageShoppingApplication().MenuButton().tap();
    }

    private void navigateToLogin() throws GeneralLeanFtException {
        if (utils.isHighlight())
            appModel.AdvantageShoppingApplication().LOGINLabel().highlight();
        appModel.AdvantageShoppingApplication().LOGINLabel().tap();
    }

    private void postInstallActions() throws  GeneralLeanFtException {
        Logging.logMessage("Checking if the 'allow' dialog is displayed...", Logging.LOG_LEVEL.INFO);
        if (appModel.HomeApplication().AllowButton().exists(3)) {
            Logging.logMessage("Tap 'Allow' app to access location", Logging.LOG_LEVEL.INFO);
            appModel.HomeApplication().AllowButton().tap();
        }
    }

    /*
     This is the main test body
    */
    private void runMainTest() throws  GeneralLeanFtException {
        Logging.logMessage ("Select 'laptop' category", Logging.LOG_LEVEL.INFO);
        if (utils.isHighlight())
            appModel.AdvantageShoppingApplication().LAPTOPSLabel().highlight();
        appModel.AdvantageShoppingApplication().LAPTOPSLabel().tap();

        Logging.logMessage ("Select a laptop", Logging.LOG_LEVEL.INFO);
        if (utils.isHighlight())
            appModel.AdvantageShoppingApplication().SelectedLaptop().highlight();
        appModel.AdvantageShoppingApplication().SelectedItem().tap();

        Logging.logMessage ("Tap 'Add to Cart' button", Logging.LOG_LEVEL.INFO);
        if (utils.isHighlight())
            appModel.AdvantageShoppingApplication().ADDTOCARTButton().highlight();
        appModel.AdvantageShoppingApplication().ADDTOCARTButton().tap();

        Logging.logMessage("Navigate to cart", Logging.LOG_LEVEL.INFO);
        if (utils.isHighlight())
            appModel.AdvantageShoppingApplication().cartIconButton().highlight();
        appModel.AdvantageShoppingApplication().cartIconButton().tap();

        Logging.logMessage ("Tap the checkout button", Logging.LOG_LEVEL.INFO);
        if (utils.isHighlight())
            appModel.AdvantageShoppingApplication().CHECKOUTButton().highlight();
        appModel.AdvantageShoppingApplication().CHECKOUTButton().tap();

        Logging.logMessage ("Tap the pay now button", Logging.LOG_LEVEL.INFO);
        if (utils.isHighlight())
            appModel.AdvantageShoppingApplication().PAYNOWButton().highlight();
        appModel.AdvantageShoppingApplication().PAYNOWButton().tap();

        Logging.logMessage ("Tap OK", Logging.LOG_LEVEL.INFO);
        if (utils.isHighlight())
            appModel.AdvantageShoppingApplication().OkButton().highlight();
        appModel.AdvantageShoppingApplication().OkButton().tap();
    }

    private void initLabResources() throws GeneralLeanFtException {
        DeviceDescription deviceDescription = new DeviceDescription();

        deviceDescription.setOsType(osType);
        deviceDescription.setOsVersion(osVersion);
        deviceDescription.setModel(osModel);
        //deviceDescription.setName("iPhone 8");

        utils.lockDevice(deviceDescription, MobileLabUtils.LabType.MC);
        //utils.lockDeviceById("ed2ff5276810f2265b87cb2d58acc7b9246aa5c4", MobileLabUtils.LabType.MC);

        device = utils.getDevice();
        if (device != null) {
            appModel = new appModel(device);
            utils.setApp();

            Logging.logMessage("Allocated device: \"" + device.getName() + "\" (" + device.getId() + "), Model :"
                    + device.getModel() + ", OS: " + device.getOSType() + " version: " + device.getOSVersion()
                    + ", manufacturer: " + device.getManufacturer(), Logging.LOG_LEVEL.INFO);

            if (utils.isInstallApp()) {
                Logging.logMessage("Installing app: " + utils.getApp().getName() + " v" + utils.getAppVersion(), Logging.LOG_LEVEL.INFO);
                utils.getApp().install();
            } else {
                Logging.logMessage("Restarting app: " + utils.getApp().getName() + " v" + utils.getAppVersion(), Logging.LOG_LEVEL.INFO);
                utils.getApp().restart();
            }
        } else {
            Logging.logMessage("Device couldn't be allocated, exiting script", Logging.LOG_LEVEL.ERROR);
            noProblem = false;
        }
    }

    private void checkIfLoggedIn() throws GeneralLeanFtException, InterruptedException {
        Logging.logMessage ("Check if the user signed in", Logging.LOG_LEVEL.INFO);
        if (appModel.AdvantageShoppingApplication().SIGNOUTLabel().exists(3)) {
            signOut();
            utils.windowSync(2000);

            Logging.logMessage ("Tap 'Open Menu (after sign-out)'", Logging.LOG_LEVEL.INFO);
            openMenu();
            utils.windowSync(2000);
        }
    }
}
