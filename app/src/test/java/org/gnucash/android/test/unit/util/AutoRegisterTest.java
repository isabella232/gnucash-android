package org.gnucash.android.test.unit.util;

import org.gnucash.android.BuildConfig;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.model.AutoRegisterProvider;
import org.gnucash.android.test.unit.testutil.GnucashTestRunner;
import org.gnucash.android.test.unit.testutil.ShadowCrashlytics;
import org.gnucash.android.test.unit.testutil.ShadowUserVoice;
import org.gnucash.android.util.AutoRegisterUtil;
import org.gnucash.android.util.AutoRegisterMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowLog;

import java.math.BigDecimal;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(GnucashTestRunner.class) //package is required so that resources can be found in dev mode
@org.robolectric.annotation.Config(constants = BuildConfig.class, sdk = 21, packageName = "org.gnucash.android",
        qualifiers = "ko-rKR",
        shadows = {ShadowCrashlytics.class, ShadowUserVoice.class})
public class AutoRegisterTest {
    private Locale mPreviousLocale;
    AutoRegisterUtil manager;

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;

        mPreviousLocale = Locale.getDefault();
        Locale.setDefault(Locale.KOREA);

        manager = GnuCashApplication.getAutoRegisterManager();
    }

    @After
    public void tearDown() throws Exception {
        Locale.setDefault(mPreviousLocale);
    }

    @Test
    public void testCountryCode() {
        assertThat(manager.findProvider("+82-1599-1111").getName()).isEqualTo("hanacard");
        assertThat(manager.findProvider("1599-1111").getName()).isEqualTo("hanacard");
        assertThat(manager.findProvider("15991111").getName()).isEqualTo("hanacard");

        assertThat(manager.findProvider("+82-1544-7200").getName()).isEqualTo("shinhancard");
        assertThat(manager.findProvider("1544-7200").getName()).isEqualTo("shinhancard");
        assertThat(manager.findProvider("15447200").getName()).isEqualTo("shinhancard");
    }

    @Test
    public void testHanaCard() {
        AutoRegisterProvider provider = manager.findProvider("15991111");
        String message = "[Web발신]\n" +
                "하나(7*7*) 진*규님 일시불 76,000원 05/07 13:41 누적 461,983원 (주)채드에이컷스";
        AutoRegisterMessage msg = provider.parseMessage(message);

        assertThat(msg.get(msg.CARDNO)).isEqualTo("7*7*");
        assertThat(msg.get(msg.HOLDER)).isEqualTo("진*규");
        assertThat(msg.get(msg.VENDOR)).isEqualTo("(주)채드에이컷스");
        assertThat(msg.get(msg.INSTALMENT)).isEqualTo("일시불");
        assertThat(msg.getBigDecimal(msg.AMOUNT)).isEqualTo(new BigDecimal("76000"));
        assertThat(msg.getBigDecimal(msg.ACCUM)).isEqualTo(new BigDecimal("461983"));
        assertThat(msg.get(msg.DATE)).isEqualTo("05/07");
        assertThat(msg.get(msg.TIME)).isEqualTo("13:41");
    }

    @Test
    public void testShinhanCard() {
        AutoRegisterProvider provider = manager.findProvider("15447200");
        String message = "[Web발신]\n" +
                "신한카드승인 진*규(6*5*) 03/05 12:35 (일시불)20,800원 네이버(주) 누적230,910원";
        AutoRegisterMessage msg = provider.parseMessage(message);

        assertThat(msg.get(msg.CARDNO)).isEqualTo("6*5*");
        assertThat(msg.get(msg.HOLDER)).isEqualTo("진*규");
        assertThat(msg.get(msg.VENDOR)).isEqualTo("네이버(주)");
        assertThat(msg.get(msg.INSTALMENT)).isEqualTo("일시불");
        assertThat(msg.getBigDecimal(msg.AMOUNT)).isEqualTo(new BigDecimal("20800"));
        assertThat(msg.getBigDecimal(msg.ACCUM)).isEqualTo(new BigDecimal("230910"));
        assertThat(msg.get(msg.DATE)).isEqualTo("03/05");
        assertThat(msg.get(msg.TIME)).isEqualTo("12:35");
    }
}