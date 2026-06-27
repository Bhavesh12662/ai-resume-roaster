package com.example.util

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.util.regex.Pattern
import java.util.zip.ZipInputStream

data class ResumeMetadata(
    val name: String?,
    val email: String?,
    val phone: String?,
    val linkedin: String?,
    val github: String?,
    val portfolio: String?,
    val hasSkills: Boolean,
    val hasEducation: Boolean,
    val hasExperience: Boolean,
    val hasProjects: Boolean,
    val hasCertifications: Boolean,
    val hasLanguages: Boolean,
    val hasAchievements: Boolean,
    val completenessScore: Int,
    val wordCount: Int,
    val charCount: Int,
    val pageCount: Int = 1
)

object ResumeParser {

    /**
     * Extracts plain text from PDF, DOCX, or TXT
     */
    fun extractText(context: Context, uri: Uri, fileName: String): Pair<String, Int> {
        val extension = fileName.lowercase().substringAfterLast('.', "")
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri) ?: throw Exception("Could not open file stream")

        return try {
            when {
                extension == "pdf" || fileName.endsWith(".pdf", ignoreCase = true) -> {
                    parsePdf(inputStream)
                }
                extension == "docx" || fileName.endsWith(".docx", ignoreCase = true) -> {
                    Pair(parseDocx(inputStream), 1)
                }
                else -> {
                    // Default to plain text
                    Pair(parseTxt(inputStream), 1)
                }
            }
        } finally {
            try {
                inputStream.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun parsePdf(inputStream: InputStream): Pair<String, Int> {
        var document: PDDocument? = null
        try {
            document = PDDocument.load(inputStream)
            if (document.isEncrypted) {
                throw Exception("The PDF file is password protected")
            }
            val pageCount = document.numberOfPages
            val stripper = PDFTextStripper()
            val text = stripper.getText(document) ?: ""
            if (text.trim().isEmpty()) {
                throw Exception("The PDF file is empty or contains only scanned images (no selectable text)")
            }
            return Pair(text, pageCount)
        } catch (e: Exception) {
            throw Exception("Failed to parse PDF: ${e.localizedMessage ?: "Corrupted file"}")
        } finally {
            document?.close()
        }
    }

    private fun parseDocx(inputStream: InputStream): String {
        try {
            val zipInputStream = ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry
            var documentXmlText = ""
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val reader = BufferedReader(InputStreamReader(zipInputStream))
                    documentXmlText = reader.readText()
                    break
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()

            if (documentXmlText.isEmpty()) {
                throw Exception("Could not locate word/document.xml in DOCX file")
            }

            // Parse documentXmlText using XmlPullParser to extract text
            val sb = StringBuilder()
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(documentXmlText))

            var eventType = parser.eventType
            var inText = false
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name
                        if (name == "p" || name == "w:p") {
                            sb.append("\n")
                        } else if (name == "t" || name == "w:t") {
                            inText = true
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inText) {
                            sb.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val name = parser.name
                        if (name == "t" || name == "w:t") {
                            inText = false
                        }
                    }
                }
                eventType = parser.next()
            }
            val text = sb.toString().trim()
            if (text.isEmpty()) {
                throw Exception("The DOCX file contains no extractable text")
            }
            return text
        } catch (e: Exception) {
            throw Exception("Failed to parse DOCX: ${e.localizedMessage ?: "Corrupted Word file"}")
        }
    }

    private fun parseTxt(inputStream: InputStream): String {
        try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val text = reader.readText().trim()
            if (text.isEmpty()) {
                throw Exception("The TXT file is empty")
            }
            return text
        } catch (e: Exception) {
            throw Exception("Failed to parse TXT file")
        }
    }

    /**
     * Parse text semantically to extract contact fields and evaluate completeness
     */
    fun parseMetadata(text: String, pageCount: Int = 1): ResumeMetadata {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        
        // Simple name detection: first line that looks like a name (short, typically 2-3 words)
        var name: String? = null
        for (line in lines.take(5)) {
            val words = line.split("\\s+".toRegex())
            if (words.size in 2..4 && !line.contains("@") && !line.contains("http") && !line.contains("linkedin") && !line.contains("+")) {
                name = line
                break
            }
        }

        // Email regex
        val emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}")
        val emailMatcher = emailPattern.matcher(text)
        val email = if (emailMatcher.find()) emailMatcher.group() else null

        // Phone regex
        val phonePattern = Pattern.compile("(\\+?[0-9][0-9\\- \\(\\)\\.]{7,16}[0-9])")
        val phoneMatcher = phonePattern.matcher(text)
        val phone = if (phoneMatcher.find()) phoneMatcher.group() else null

        // LinkedIn & GitHub & Portfolio URL matchers
        val linkedinPattern = Pattern.compile("(linkedin\\.com/[a-zA-Z0-9\\-_/]+)")
        val linkedinMatcher = linkedinPattern.matcher(text)
        val linkedin = if (linkedinMatcher.find()) linkedinMatcher.group() else null

        val githubPattern = Pattern.compile("(github\\.com/[a-zA-Z0-9\\-_/]+)")
        val githubMatcher = githubPattern.matcher(text)
        val github = if (githubMatcher.find()) githubMatcher.group() else null

        // Portfolio: portfolio domains or custom sites
        val portfolioPattern = Pattern.compile("((behance\\.net/[a-zA-Z0-9\\-_]+)|(dribbble\\.com/[a-zA-Z0-9\\-_]+)|((?:https?://)?(?:www\\.)?[a-zA-Z0-9\\-_]+\\.(?:me|io|com|net|org)(?!/github)(?!/linkedin)(?:/[a-zA-Z0-9\\-_]+)*))")
        val portfolioMatcher = portfolioPattern.matcher(text.lowercase())
        val portfolio = if (portfolioMatcher.find()) portfolioMatcher.group() else null

        // Check sections via keywords
        val lowerText = text.lowercase()
        val hasSkills = lowerText.contains("skill") || lowerText.contains("technolog") || lowerText.contains("languages") || lowerText.contains("expertise")
        val hasEducation = lowerText.contains("education") || lowerText.contains("academic") || lowerText.contains("university") || lowerText.contains("college") || lowerText.contains("degree")
        val hasExperience = lowerText.contains("experience") || lowerText.contains("employment") || lowerText.contains("history") || lowerText.contains("work") || lowerText.contains("career")
        val hasProjects = lowerText.contains("project") || lowerText.contains("portfolio")
        val hasCertifications = lowerText.contains("certif") || lowerText.contains("licenses") || lowerText.contains("credentials")
        val hasLanguages = lowerText.contains("languages") || lowerText.contains("bilingual") || lowerText.contains("fluent")
        val hasAchievements = lowerText.contains("achievement") || lowerText.contains("award") || lowerText.contains("honors")

        // Score calculation: max 100
        var score = 0
        if (!name.isNullOrEmpty()) score += 10
        if (!email.isNullOrEmpty()) score += 10
        if (!phone.isNullOrEmpty()) score += 10
        if (!linkedin.isNullOrEmpty()) score += 5
        if (!github.isNullOrEmpty()) score += 5
        if (!portfolio.isNullOrEmpty()) score += 5
        if (hasSkills) score += 15
        if (hasExperience) score += 15
        if (hasEducation) score += 10
        if (hasProjects) score += 10
        if (hasCertifications) score += 5

        val finalScore = score.coerceIn(15, 100) // minimum score is 15 if any text exists

        val wordCount = text.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
        val charCount = text.length

        return ResumeMetadata(
            name = name,
            email = email,
            phone = phone,
            linkedin = linkedin,
            github = github,
            portfolio = portfolio,
            hasSkills = hasSkills,
            hasEducation = hasEducation,
            hasExperience = hasExperience,
            hasProjects = hasProjects,
            hasCertifications = hasCertifications,
            hasLanguages = hasLanguages,
            hasAchievements = hasAchievements,
            completenessScore = finalScore,
            wordCount = wordCount,
            charCount = charCount,
            pageCount = pageCount
        )
    }
}
