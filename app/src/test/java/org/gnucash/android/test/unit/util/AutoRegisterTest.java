package org.gnucash.android.test.unit.util;

import org.gnucash.android.BuildConfig;
import org.gnucash.android.db.adapter.AutoRegisterProviderDbAdapter;
import org.gnucash.android.model.AutoRegister;
import org.gnucash.android.model.Money;
import org.gnucash.android.test.unit.testutil.GnucashTestRunner;
import org.gnucash.android.test.unit.testutil.ShadowCrashlytics;
import org.gnucash.android.test.unit.testutil.ShadowUserVoice;
import org.gnucash.android.util.AutoRegisterUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowLog;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(GnucashTestRunner.class) //package is required so that resources can be found in dev mode
@org.robolectric.annotation.Config(constants = BuildConfig.class, sdk = 21, packageName = "org.gnucash.android",
        qualifiers = "ko-rKR",
        shadows = {ShadowCrashlytics.class, ShadowUserVoice.class})
public class AutoRegisterTest {
    private Locale mPreviousLocale;
    private AutoRegisterProviderDbAdapter mProviderDbAdapter;
    private List<AutoRegister.Provider> mEnabledProviders;

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;

        mPreviousLocale = Locale.getDefault();
        Locale.setDefault(Locale.KOREA);

        mProviderDbAdapter = AutoRegisterProviderDbAdapter.getInstance();
        AutoRegister.Provider p = mProviderDbAdapter.getRecord(1L);
        String dummyAccountUID = p.getUID();
        mProviderDbAdapter.setActive(p.getUID(), dummyAccountUID);
        mEnabledProviders = mProviderDbAdapter.getProvidersByStatus(true);
    }

    @After
    public void tearDown() throws Exception {
        Locale.setDefault(mPreviousLocale);
    }

    @Test
    public void testFindProvider() {
        assertThat(AutoRegisterUtil.findProvider(mEnabledProviders, "+82-1800-1111").getName()).isEqualTo("하나카드");
        assertThat(AutoRegisterUtil.findProvider(mEnabledProviders, "1800-1111").getName()).isEqualTo("hanacard");
        assertThat(AutoRegisterUtil.findProvider(mEnabledProviders, "18001111").getName()).isEqualTo("hanacard");
    }

    @Test
    public void testHanaCard() {
        AutoRegister.Provider provider = AutoRegisterUtil.findProvider(mEnabledProviders, "18001111");
        String message = "[Web발신]\n" +
                "하나(7*7*) 진*규님 일시불 76,000원 05/07 13:41 누적 461,983원 (주)채드에이컷스";
        AutoRegister.Inbox msg = provider.parseMessage(message);

        assertThat(msg.getCardNo()).isEqualTo("7*7*");
        assertThat(msg.getHolder()).isEqualTo("진*규");
        assertThat(msg.getMemo()).isEqualTo("(주)채드에이컷스");
        assertThat(msg.getInstalment()).isEqualTo("일시불");
        assertThat(msg.getAmount()).isEqualTo(new Money("76000", "KRW"));
        assertThat(msg.getAccum()).isEqualTo(new Money("461983", "KRW"));
/*
        assertThat(msg.get(msg.DATE)).isEqualTo("05/07");
        assertThat(msg.get(msg.TIME)).isEqualTo("13:41");
*/
    }

    @Test
    public void testShinhanCard() {
/*
        Provider provider = AutoRegisterUtil.findProvider("15447200");
        String message = "[Web발신]\n" +
                "신한카드승인 진*규(6*5*) 03/05 12:35 (일시불)20,800원 네이버(주) 누적230,910원";
        Inbox msg = provider.parseMessage(message);

        assertThat(msg.get(msg.CARDNO)).isEqualTo("6*5*");
        assertThat(msg.get(msg.HOLDER)).isEqualTo("진*규");
        assertThat(msg.get(msg.VENDOR)).isEqualTo("네이버(주)");
        assertThat(msg.get(msg.INSTALMENT)).isEqualTo("일시불");
        assertThat(msg.getBigDecimal(msg.AMOUNT)).isEqualTo(new BigDecimal("20800"));
        assertThat(msg.getBigDecimal(msg.ACCUM)).isEqualTo(new BigDecimal("230910"));
        assertThat(msg.get(msg.DATE)).isEqualTo("03/05");
        assertThat(msg.get(msg.TIME)).isEqualTo("12:35");
*/
    }
}