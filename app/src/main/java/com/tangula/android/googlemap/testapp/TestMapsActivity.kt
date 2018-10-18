package com.tangula.android.googlemap.testapp

import android.os.Bundle
import com.tangula.android.googlemap.TglBasicGoogleMapActivity
import com.tangula.android.utils.ApplicationUtils

class TestMapsActivity : TglBasicGoogleMapActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ApplicationUtils.APP = this.application
        this.supplier4ContentLayoutResId={R.layout.activity_test_maps}
        this.supplier4MapFragmentResId={R.id.map}

        super.onCreate(savedInstanceState)

    }



}
