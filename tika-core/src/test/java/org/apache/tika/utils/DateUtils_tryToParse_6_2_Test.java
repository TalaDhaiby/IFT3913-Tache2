/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DateUtils_tryToParse_6_2_Test {

    private DateUtils dateUtils;

    @BeforeEach
    public void setUp() {
        dateUtils = new DateUtils();
    }

    @Test
    public void testTryToParseNull() {
        String dateString = null;
        Date result = dateUtils.tryToParse(dateString);
        assertNull(result);
    }

    @Test
    public void testTryToParseShortString() {
        String dateString = "2023";
        Date result = dateUtils.tryToParse(dateString);
        assertNull(result);
    }

    @Test
    public void testTryToParseValidDate() throws ParseException {
        String dateString = "2023-04-15";
        Date result = dateUtils.tryToParse(dateString);
        assertEquals(Date.from(Instant.parse("2023-04-15T12:00:00Z")), result);
    }

    @Test
    public void testTryToParseValidDateWithTime() throws ParseException {
        String dateString = "2023-04-15T12:34:56";
        Date result = dateUtils.tryToParse(dateString);
        assertEquals(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(dateString), result);
    }

    @Test
    public void testTryToParseInvalidDate() throws ParseException {
        String dateString = "2023-04-15-12:34:56";
        Date result = dateUtils.tryToParse(dateString);
        assertEquals(Date.from(Instant.parse("2023-04-15T12:00:00Z")), result);
    }

    @Test
    public void testTryToParseInvalidDateWithTime() throws ParseException {
        String dateString = "2023-04-15T12:34:56-12:34:56";
        Date result = dateUtils.tryToParse(dateString);
        Date expected = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                .parse("2023-04-15T12:34:56");
        assertEquals(expected, result);
    }

    @Test
    public void testTryToParseInvalidDateWithoutTime() throws ParseException {
        String dateString = "2023-04-15-12:34:56";
        Date result = dateUtils.tryToParse(dateString);
        assertEquals(Date.from(Instant.parse("2023-04-15T12:00:00Z")), result);
    }

    @Test
    public void testTryToParseInvalidDateWithoutTimeAndTimezone() throws ParseException {
        String dateString = "2023-04-15-12:34:56";
        Date result = dateUtils.tryToParse(dateString);
        assertEquals(Date.from(Instant.parse("2023-04-15T12:00:00Z")), result);
    }
}
