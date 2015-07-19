package com.ashbysoft.felixstoweradio;

import android.test.AndroidTestCase;

import org.joda.time.DateTime;

public class ApplicationTest extends AndroidTestCase {

    public void testServiceInfoParser() throws Exception {
        ServiceInformationParser sip = new ServiceInformationParser(getContext());
        String xml = Utils.getStringFromRaw(getContext(), R.raw.sample_si2);

        sip.parse(xml, DateTime.parse("2015-07-17T20:15:00+01:00"));
        assertEquals("Capital Breakfast", sip.getCurrentProgramme());
        assertEquals("Capital Lunch", sip.getNextProgramme());

        sip.parse(xml, DateTime.parse("2015-07-17T15:15:00+01:00"));
        assertEquals("", sip.getCurrentProgramme());
        assertEquals("Capital Dawn", sip.getNextProgramme());

        sip.parse(xml, DateTime.parse("2015-07-18T01:15:00+01:00"));
        assertEquals("Capital Lunch", sip.getCurrentProgramme());
        assertEquals("", sip.getNextProgramme());
    }
}