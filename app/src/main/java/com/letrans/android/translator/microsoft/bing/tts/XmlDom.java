package com.letrans.android.translator.microsoft.bing.tts;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class XmlDom {

    public static String createDom(String locale, String genderName, String voiceName, String volume, String textToSynthesize) {
        Document doc = null;
        Element speak, voice, prosody;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            doc = builder.newDocument();
            if (doc != null){
                speak = doc.createElement("speak");
                speak.setAttribute("version", "1.0");
                speak.setAttribute("xml:lang", locale);
                voice = doc.createElement("voice");
                voice.setAttribute("xml:lang", locale);
                voice.setAttribute("xml:gender", genderName);
                voice.setAttribute("name", voiceName);
                prosody = doc.createElement("prosody");
                prosody.setAttribute("volume", volume);
                //voice.appendChild(doc.createTextNode(textToSynthesize));
                prosody.appendChild(doc.createTextNode(textToSynthesize));
                voice.appendChild(prosody);
                speak.appendChild(voice);
                doc.appendChild(speak);
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        return transformDom(doc);
    }

    private static String transformDom(Document doc){
        StringWriter writer = new StringWriter();
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer;
            transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return writer.getBuffer().toString().replaceAll("\n|\r", "");
    }
}