/*
 * Copyright 2003-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.runtime

import java.text.DateFormat
import java.text.SimpleDateFormat

/**
 * @author tnichols
 * @author Paul King
 */
class DateGDKTest extends GroovyTestCase {

    void testGDKDateMethods() {
        Locale defaultLocale = Locale.default
        TimeZone defaultTZ = TimeZone.default
        Locale locale = Locale.UK
        Locale.setDefault locale // set this otherwise the test will fail if your locale isn't the same
        TimeZone.setDefault TimeZone.getTimeZone('Etc/GMT')
        Date d = new Date(0)
        assertEquals '1970-01-01', d.format('yyyy-MM-dd')
        assertEquals '01/Jan/1970', d.format('dd/MMM/yyyy', TimeZone.getTimeZone('GMT'))
        assertEquals DateFormat.getDateInstance(DateFormat.SHORT, locale).format(d), d.dateString
        assertEquals '01/01/70', d.dateString
        assertEquals DateFormat.getTimeInstance(DateFormat.MEDIUM, locale).format(d), d.timeString
        assertEquals '00:00:00', d.timeString
        assertEquals DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale).format(d), d.dateTimeString
        assertEquals '01/01/70 00:00:00', d.dateTimeString

        Locale.default = defaultLocale
        TimeZone.setDefault defaultTZ
    }

    void testStaticParse() {
        TimeZone defaultTZ = TimeZone.default
        TimeZone.setDefault TimeZone.getTimeZone('Etc/GMT')

        Date d = Date.parse('yy/MM/dd hh:mm:ss', '70/01/01 00:00:00')
        assertEquals 0, d.time

        TimeZone.setDefault defaultTZ
    }

    void testParseWithTimeZone() {
        TimeZone defaultTZ = TimeZone.default

        TimeZone.default = TimeZone.getTimeZone("GMT+05")
        def tz = TimeZone.getTimeZone("GMT+03")
        def newYear = Date.parse('yyyy-MM-dd', "2015-01-01", tz)
        assert newYear.toString() == 'Thu Jan 01 02:00:00 GMT+05:00 2015'

        TimeZone.default = defaultTZ
    }

    void testRoundTrip() {
        Date d = new Date()
        String pattern = 'dd MMM yyyy, hh:mm:ss,SSS a z'
        String out = d.format(pattern)

        Date d2 = Date.parse(pattern, out)

        assertEquals d.time, d2.time
    }

    void testCalendarTimeZone() {
        Locale defaultLocale = Locale.default
        TimeZone defaultTZ = TimeZone.default
        Locale locale = Locale.UK
        Locale.setDefault locale // set this otherwise the test will fail if your locale isn't the same
        TimeZone.setDefault TimeZone.getTimeZone('Etc/GMT')

        def offset = 8
        def notLocalTZ = TimeZone.getTimeZone("GMT-$offset")
        Calendar cal = Calendar.getInstance(notLocalTZ)
        def offsetHr = cal.format('HH') as int
        def hr = cal.time.format('HH') as int

        if (hr < offset) hr += 24 // if GMT hr has rolled over to next day
        // offset should be 8 hours behind GMT:
        assertEquals(offset, hr - offsetHr)

        Locale.default = defaultLocale
        TimeZone.setDefault defaultTZ
    }

    static SimpleDateFormat f = new SimpleDateFormat('MM/dd/yyyy')

    static java.sql.Date sqlDate(String s) {
        return new java.sql.Date(f.parse(s).time)
    }

    void testMinusDates() {
        assertEquals(10, f.parse("1/11/2007") - f.parse("1/1/2007"))
        assertEquals(-10, f.parse("1/1/2007") - f.parse("1/11/2007"))
        assertEquals(375, f.parse("1/11/2008") - f.parse("1/1/2007"))
        assertEquals(356, f.parse("1/1/2008") - f.parse("1/10/2007"))
        assertEquals(1, f.parse("7/12/2007") - f.parse("7/11/2007"))
        assertEquals(0, f.parse("1/1/2007") - f.parse("1/1/2007"))
        assertEquals(-1, f.parse("12/31/2007") - f.parse("1/1/2008"))
        assertEquals(365, f.parse("1/1/2008") - f.parse("1/1/2007"))
        assertEquals(36525, f.parse("1/1/2008") - f.parse("1/1/1908"))

        assertEquals(1, sqlDate("7/12/2007") - f.parse("7/11/2007"))
        assertEquals(0, sqlDate("1/1/2007") - sqlDate("1/1/2007"))
        assertEquals(-1, f.parse("12/31/2007") - sqlDate("1/1/2008"))
        assertEquals(365, sqlDate("1/1/2008") - sqlDate("1/1/2007"))
        assertEquals(36525, f.parse("1/1/2008") - sqlDate("1/1/1908"))

        Date d = f.parse("7/4/1776");
        assertEquals(44, (d + 44) - d);

        java.sql.Date sqld = sqlDate("7/4/1776");
        assertEquals(-4444, (sqld - 4444) - sqld);
    }

    /** GROOVY-3374  */
    void testClearTime() {
        def now = new Date()
        def calendarNow = Calendar.getInstance()

        now.clearTime()
        calendarNow.clearTime()

        assert now == calendarNow.time

        assert calendarNow.get(Calendar.HOUR) == 0
        assert calendarNow.get(Calendar.MINUTE) == 0
        assert calendarNow.get(Calendar.SECOND) == 0
        assert calendarNow.get(Calendar.MILLISECOND) == 0
    }

    /** GROOVY-4789 */
    void testStaticParseToStringDate() {
        TimeZone tz = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("CET"))
        Date date = new Date(0)
        String toStringRepresentation = date.toString()
        assert toStringRepresentation == "Thu Jan 01 01:00:00 CET 1970"
        assert date == Date.parseToStringDate(toStringRepresentation)
        TimeZone.setDefault(tz)
    }

    void test_Upto_Date_ShouldExecuteClosureForEachDayUpToDate() {
        Date startDate = new Date()
        List expectedResults = [startDate, startDate + 1, startDate + 2]

        List actualResults = []

        startDate.upto(startDate + 2){
            actualResults << it
        }

        assert actualResults == expectedResults
    }

    void test_upto_Date_ShouldNotAcceptToDatesLessThanStartDate() {
        Date startDate = new Date()
        Date toDate = new Date(startDate.getTime() - 1L)

        shouldFail(GroovyRuntimeException) {
            startDate.upto(toDate){}
        }
    }

    void test_downto_Date_ShouldExecuteClosureForEachDayDownToDate() {
        Date startDate = new Date()
        List expectedResults = [startDate, startDate - 1, startDate - 2]

        List actualResults = []
        startDate.downto(startDate - 2){
            actualResults << it
        }

        assert actualResults == expectedResults
    }

    void test_downto_Date_ShouldNotAcceptToDatesGreaterThanStartDate() {
        Date startDate = new Date()
        Date toDate = new Date(startDate.getTime() + 1L)

        shouldFail(GroovyRuntimeException) {
            startDate.downto(toDate){}
        }
    }

    void test_upto_Calendar_ShouldExecuteClosureForEachDayUpToDate() {
        Calendar startDate = Calendar.getInstance()
        Calendar toDate = startDate.clone()
        toDate.add(Calendar.DATE, 1)
        List expectedResults = [startDate, toDate]

        List actualResults = []
        startDate.upto(toDate){
            actualResults << it
        }

        assert actualResults == expectedResults
    }

    void test_upto_Calendar_ShouldNotAcceptToDatesLessThanStartDate() {
        Calendar startDate = Calendar.getInstance()
        Calendar toDate = startDate.clone()
        toDate.add(Calendar.MILLISECOND, -1)

        shouldFail(GroovyRuntimeException) {
            startDate.upto(toDate){}
        }
    }

    void test_downto_Calendar_ShouldExecuteClosureForEachDayDownToDate() {
        Calendar startDate = Calendar.getInstance()
        Calendar toDate = startDate.clone()
        toDate.add(Calendar.DATE, -1)
        List expectedResults = [startDate, toDate]

        List actualResults = []
        startDate.downto(toDate){
            actualResults << it
        }

        assert actualResults == expectedResults
    }

    void test_downto_Calendar_ShouldNotAcceptToDatesGreaterThanStartDate() {
        Calendar startDate = Calendar.getInstance()
        Calendar toDate = startDate.clone()
        toDate.add(Calendar.MILLISECOND, 1)

        shouldFail(GroovyRuntimeException) {
            startDate.downto(toDate){}
        }
    }

    void testCopyWith() {
        Date febOne1970 = new Date(70, 1, 1)
        Date aprilSix = febOne1970.copyWith(dayOfMonth: 6, month: Calendar.APRIL)
        assertEquals '1970-04-06', aprilSix.format('yyyy-MM-dd')
        Map updates = [:]
        updates[Calendar.DAY_OF_MONTH] = 4
        Date aprilFour = aprilSix.copyWith(updates)
        assertEquals '1970-04-04', aprilFour.format('yyyy-MM-dd')
    }
}
