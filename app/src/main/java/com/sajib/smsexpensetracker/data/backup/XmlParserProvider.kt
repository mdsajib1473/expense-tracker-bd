package com.sajib.smsexpensetracker.data.backup

import org.xmlpull.v1.XmlPullParser

/**
 * Creates a fresh [XmlPullParser] for each import. Production binds the
 * platform parser (android.util.Xml); JVM unit tests supply kxml2 2.3.0, the
 * library the platform parser is derived from. One known difference: kxml2
 * truncates a character reference above U+FFFF to a single char, while the
 * platform parser decodes it correctly.
 */
fun interface XmlParserProvider {

    /** Returns a new parser that has not been given any input yet. */
    fun newParser(): XmlPullParser
}
