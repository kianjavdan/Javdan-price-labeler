package com.javdan.pricelabeler;

import android.content.Context;
import android.net.Uri;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class XlsxReader {

    private final Context context;

    public XlsxReader(Context c) {
        context = c;
    }

    public ArrayList<LinkedHashMap<String, String>> readFirstSheet(Uri uri) throws Exception {

        File temp = new File(
                context.getCacheDir(),
                "javdan_excel_" + System.currentTimeMillis() + ".xlsx"
        );

        try (
                InputStream in = context.getContentResolver().openInputStream(uri);
                OutputStream out = new FileOutputStream(temp)
        ) {
            if (in == null) {
                throw new IOException("فایل Excel باز نشد.");
            }

            byte[] buf = new byte[8192];
            int n;

            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        }

        try (ZipFile z = new ZipFile(temp)) {

            ArrayList<String> shared = readShared(z);

            ZipEntry sheet = z.getEntry("xl/worksheets/sheet1.xml");

            if (sheet == null) {
                throw new IOException("Sheet اول پیدا نشد.");
            }

            Document d;

            try (InputStream sheetIn = z.getInputStream(sheet)) {
                d = parse(sheetIn);
            }

            NodeList rows = d.getElementsByTagName("row");

            ArrayList<ArrayList<String>> matrix = new ArrayList<>();

            for (int i = 0; i < rows.getLength(); i++) {

                Element row = (Element) rows.item(i);
                NodeList cells = row.getElementsByTagName("c");

                ArrayList<String> vals = new ArrayList<>();

                int expected = 0;

                for (int j = 0; j < cells.getLength(); j++) {

                    Element c = (Element) cells.item(j);

                    String ref = c.getAttribute("r");
                    int col = colIndex(ref);

                    while (expected < col) {
                        vals.add("");
                        expected++;
                    }

                    String type = c.getAttribute("t");
                    String value = "";

                    NodeList vs = c.getElementsByTagName("v");

                    if (vs.getLength() > 0) {
                        value = vs.item(0).getTextContent();
                    }

                    if ("s".equals(type) && !value.isEmpty()) {

                        int idx = Integer.parseInt(value);

                        value =
                                (idx >= 0 && idx < shared.size())
                                        ? shared.get(idx)
                                        : "";

                    } else if ("inlineStr".equals(type)) {

                        NodeList ts = c.getElementsByTagName("t");

                        value =
                                ts.getLength() > 0
                                        ? ts.item(0).getTextContent()
                                        : "";
                    }

                    vals.add(value);
                    expected++;
                }

                matrix.add(vals);
            }

            if (matrix.isEmpty()) {
                return new ArrayList<>();
            }

            ArrayList<String> headers = matrix.get(0);

            ArrayList<LinkedHashMap<String, String>> out = new ArrayList<>();

            for (int i = 1; i < matrix.size(); i++) {

                LinkedHashMap<String, String> m = new LinkedHashMap<>();
                ArrayList<String> row = matrix.get(i);

                for (int j = 0; j < headers.size(); j++) {

                    String h = headers.get(j);

                    if (h == null || h.trim().isEmpty()) {
                        h = "Column_" + (j + 1);
                    }

                    m.put(
                            h,
                            j < row.size()
                                    ? row.get(j)
                                    : ""
                    );
                }

                out.add(m);
            }

            return out;

        } finally {
            //noinspection ResultOfMethodCallIgnored
            temp.delete();
        }
    }

    private ArrayList<String> readShared(ZipFile z) throws Exception {

        ArrayList<String> out = new ArrayList<>();

        ZipEntry e = z.getEntry("xl/sharedStrings.xml");

        if (e == null) {
            return out;
        }

        Document d;

        try (InputStream sharedIn = z.getInputStream(e)) {
            d = parse(sharedIn);
        }

        NodeList sis = d.getElementsByTagName("si");

        for (int i = 0; i < sis.getLength(); i++) {

            Element si = (Element) sis.item(i);
            NodeList ts = si.getElementsByTagName("t");

            StringBuilder s = new StringBuilder();

            for (int j = 0; j < ts.getLength(); j++) {
                s.append(ts.item(j).getTextContent());
            }

            out.add(s.toString());
        }

        return out;
    }

    /*
     * Android's XML parser does not support every Apache/Xerces feature URI.
     * Previously, setFeature(disallow-doctype-decl) threw immediately.
     *
     * We now:
     * 1) Reject any XML containing a DOCTYPE before parsing.
     * 2) Apply hardening flags only when the current Android parser supports them.
     * 3) Block all external entity resolution with an EntityResolver.
     *
     * This keeps XLSX parsing compatible with Android without allowing external XML resources.
     */
    private Document parse(InputStream in) throws Exception {

        byte[] xml = readAll(in);

        String probe = new String(
                xml,
                0,
                Math.min(xml.length, 8192),
                StandardCharsets.UTF_8
        ).toUpperCase();

        if (probe.contains("<!DOCTYPE")) {
            throw new IOException("فایل Excel شامل XML غیرمجاز (DOCTYPE) است.");
        }

        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();

        f.setNamespaceAware(false);
        f.setExpandEntityReferences(false);

        trySetFeature(
                f,
                "http://apache.org/xml/features/disallow-doctype-decl",
                true
        );

        trySetFeature(
                f,
                "http://xml.org/sax/features/external-general-entities",
                false
        );

        trySetFeature(
                f,
                "http://xml.org/sax/features/external-parameter-entities",
                false
        );

        trySetFeature(
                f,
                "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                false
        );

        trySetAttribute(
                f,
                XMLConstants.ACCESS_EXTERNAL_DTD,
                ""
        );

        trySetAttribute(
                f,
                XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                ""
        );

        DocumentBuilder b = f.newDocumentBuilder();

        b.setEntityResolver((publicId, systemId) ->
                new InputSource(new ByteArrayInputStream(new byte[0]))
        );

        return b.parse(new ByteArrayInputStream(xml));
    }

    private void trySetFeature(
            DocumentBuilderFactory f,
            String name,
            boolean value
    ) {
        try {
            f.setFeature(name, value);
        } catch (Throwable ignored) {
            // Some Android XML implementations do not support this feature.
        }
    }

    private void trySetAttribute(
            DocumentBuilderFactory f,
            String name,
            String value
    ) {
        try {
            f.setAttribute(name, value);
        } catch (Throwable ignored) {
            // Not supported on every Android API/parser implementation.
        }
    }

    private byte[] readAll(InputStream in) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte[] buf = new byte[8192];
        int n;

        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }

        return out.toByteArray();
    }

    private int colIndex(String ref) {

        int n = 0;

        for (int i = 0; i < ref.length(); i++) {

            char ch = ref.charAt(i);

            if (ch >= 'A' && ch <= 'Z') {
                n = n * 26 + (ch - 'A' + 1);
            } else {
                break;
            }
        }

        return Math.max(0, n - 1);
    }
}
