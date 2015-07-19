package com.ashbysoft.felixstoweradio;

import android.content.Context;

import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryEvaluator;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.transform.stream.StreamSource;

public class ServiceInformationParser {
    // Required for loading anything from resources
    private Context context = null;

    private String currentProgramme = null;
    private String nextProgramme = null;

    public ServiceInformationParser(Context context) {
        this.context = context;
    }

    /**
     * @param xml XML document containing RadioDNS Service Information as per spec:
     *            http://www.etsi.org/deliver/etsi_ts/102800_102899/102818/03.01.01_60/ts_102818v030101p.pdf
     * @param now The current date and time for the purpose of determining what is next and what is now playing.
     * @throws IOException if an error occurs reading xQueries from file, or if parse error occurs.
     */
    public void parse(String xml, DateTime now) throws IOException {
        try {
            DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime();
            String nowString = "\"" + dateTimeFormatter.print(now) + "\"";
            currentProgramme = runXQueryGetTextResult(xml, R.raw.now_playing, nowString);
            nextProgramme = runXQueryGetTextResult(xml, R.raw.next_playing, nowString);
        } catch (SaxonApiException e) {
            throw new IOException("Failed to parse XML document", e);
        }
    }

    private String runXQueryGetTextResult(String xml, int xQueryResource, String... formatArgs) throws IOException, SaxonApiException {
        String xQuery = Utils.getStringFromRaw(context, xQueryResource);
        xQuery = String.format(xQuery, formatArgs);
        return runXQueryGetTextResult(xml, xQuery);
    }

    /**
     * Use Saxon API to run xQuery on xml, returns results as plain text.
     * http://www.saxonica.com/documentation/index.html#!using-xquery/api-query/s9api-query
     */
    private String runXQueryGetTextResult(String xml, String xQuery) throws IOException, SaxonApiException {
        Configuration config = new Configuration();

        Processor processor = new Processor(config);

        byte[] inputBytes = xml.getBytes("UTF-8");
        ByteArrayInputStream bais = new ByteArrayInputStream(inputBytes);
        StreamSource streamSource = new StreamSource();
        streamSource.setInputStream(bais);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Serializer destination = processor.newSerializer(baos);
        destination.setOutputProperty(Serializer.Property.METHOD, "text");

        XQueryCompiler xQueryCompiler = processor.newXQueryCompiler();
        XQueryEvaluator xQueryEvaluator = xQueryCompiler.compile(xQuery).load();

        xQueryEvaluator.setSource(streamSource);
        xQueryEvaluator.setDestination(destination);
        xQueryEvaluator.run();

        byte[] bytes = baos.toByteArray();
        return new String(bytes, "UTF-8");
    }

    public String getCurrentProgramme() {
        return currentProgramme;
    }

    public String getNextProgramme() {
        return nextProgramme;
    }
}
