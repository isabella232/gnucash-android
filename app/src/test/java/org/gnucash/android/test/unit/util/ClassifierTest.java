package org.gnucash.android.test.unit.util;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.Normalize;

import static org.junit.Assert.assertTrue;

/**
 * Created by hkjinlee on 2017. 5. 22..
 */
@RunWith(JUnit4.class)
public class ClassifierTest {
    private static ArrayList<Attribute> ATTRIBUTES;

    private enum Attr {
        HEIGHT, WEIGHT, GENDER;
    }

    private enum Gender {
        MALE, FEMALE, ALIEN;
    }

    private Instances data;
    private Instances normalized;
    private Instances binarized;
    private Instances mekaData;

    @BeforeClass
    public static void setUpClass() {
        List<String> genderDomain = new ArrayList<>();
        for (Gender g : Gender.values()) {
            genderDomain.add(g.name());
        }

        ATTRIBUTES = new ArrayList<>();
        ATTRIBUTES.add(Attr.HEIGHT.ordinal(), new Attribute(Attr.HEIGHT.name()));
        ATTRIBUTES.add(Attr.WEIGHT.ordinal(), new Attribute(Attr.WEIGHT.name()));
        ATTRIBUTES.add(Attr.GENDER.ordinal(), new Attribute(Attr.GENDER.name(), genderDomain));
    }

    private static Instances normalizeData(Instances data) throws Exception {
        Filter normalize = new Normalize();
        normalize.setInputFormat(data);

        return Filter.useFilter(data, normalize);
    }

    @Before
    public void setUp() throws Exception {
        data = new Instances("people", ATTRIBUTES, 10);
        //data.setClassIndex(Attr.GENDER.ordinal());

        data.add(createRow(185, 80, Gender.MALE));
        data.add(createRow(170, 90, Gender.MALE));
        data.add(createRow(175, 60, Gender.MALE));
        data.add(createRow(173, 75, Gender.MALE));
        data.add(createRow(180, 99, Gender.MALE));

        data.add(createRow(165, 45, Gender.FEMALE));
        data.add(createRow(150, 50, Gender.FEMALE));
        data.add(createRow(155, 55, Gender.FEMALE));
        data.add(createRow(163, 53, Gender.FEMALE));
        data.add(createRow(167, 60, Gender.FEMALE));

        data.add(createRow(40, 10, Gender.ALIEN));
        data.add(createRow(300, 120, Gender.ALIEN));

        normalized = normalizeData(data);

        Instances noIndex = new Instances(normalized);
        noIndex.setClassIndex(-1);
        NominalToBinary n2b = new NominalToBinary();
        n2b.setOptions(new String[] { "-A" });
        n2b.setInputFormat(noIndex);
        binarized = Filter.useFilter(noIndex, n2b);

    }

    private Instance createRow(int height, int weight, Gender gender) {
        Instance i = new DenseInstance(ATTRIBUTES.size());

        i.setDataset(data);
        i.setValue(Attr.GENDER.ordinal(), gender.name());
        i.setValue(Attr.HEIGHT.ordinal(), height);
        i.setValue(Attr.WEIGHT.ordinal(), weight);

        return i;
    }

    @Test
    public void testNormalizeFilter() throws Exception {
        int heightIndex = Attr.HEIGHT.ordinal();

        assertTrue(normalized.attributeStats(heightIndex).numericStats.max == 1);
        assertTrue(normalized.attributeStats(heightIndex).numericStats.min == 0);
    }

    @Test
    public void testBinarizeFilter() throws Exception {
        System.out.println(binarized);
    }

    @Test
    public void testBatchClassifier() throws Exception {
        J48 j48 = new J48();
        j48.buildClassifier(normalized);
    }
}
