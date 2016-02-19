package com.aviadmini.nogamenolife;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.test.ApplicationTestCase;
import android.test.MoreAsserts;

public class AppTest
        extends ApplicationTestCase<Application> {

    private Application mApp;

    public AppTest() {
        super(Application.class);
    }

    @Override
    protected void setUp()
            throws Exception {
        super.setUp();

        this.createApplication();

        this.mApp = this.getApplication();

    }

    public void testVersion()
            throws Exception {

        final PackageInfo packageInfo = this.mApp.getPackageManager()
                                                 .getPackageInfo(this.mApp.getPackageName(), 0);

        assertNotNull("installed incorrectly", packageInfo);

        MoreAsserts.assertMatchesRegex("\\d+\\.\\d+\\.\\d+", packageInfo.versionName);

    }

}