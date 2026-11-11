package com.manikandan.data.mediaservice

import com.manikandan.domain.data.model.browse.album.Track
import com.manikandan.logger.Logger

/**
 * Intelligent language detection and compatibility filtering for endless queue, radio, and song skipping.
 * Ensures the queue strictly plays songs in the same regional language according to user listening.
 */
object TrackLanguageFilter {
    private const val TAG = "TrackLanguageFilter"

    // Primary language codes
    const val LANG_TAMIL = "ta"
    const val LANG_TELUGU = "te"
    const val LANG_HINDI = "hi"
    const val LANG_MALAYALAM = "ml"
    const val LANG_KANNADA = "kn"
    const val LANG_PUNJABI = "pa"
    const val LANG_BENGALI = "bn"
    const val LANG_KOREAN = "ko"
    const val LANG_JAPANESE = "ja"
    const val LANG_SPANISH = "es"
    const val LANG_ENGLISH = "en"
    const val LANG_UNKNOWN = "unknown"

    // Unicode script ranges
    private val SCRIPT_RANGES = listOf(
        LANG_TAMIL to ('\u0B80'..'\u0BFF'),
        LANG_TELUGU to ('\u0C00'..'\u0C7F'),
        LANG_MALAYALAM to ('\u0D00'..'\u0D7F'),
        LANG_KANNADA to ('\u0C80'..'\u0CFF'),
        LANG_HINDI to ('\u0900'..'\u097F'),
        LANG_PUNJABI to ('\u0A00'..'\u0A7F'),
        LANG_BENGALI to ('\u0980'..'\u09FF'),
        LANG_KOREAN to ('\uAC00'..'\uD7AF'),
        LANG_KOREAN to ('\u1100'..'\u11FF'),
        LANG_JAPANESE to ('\u3040'..'\u309F'),
        LANG_JAPANESE to ('\u30A0'..'\u30FF'),
    )

    // Language keyword patterns
    private val KEYWORD_PATTERNS = listOf(
        LANG_TAMIL to Regex("(?i)\\b(tamil|tamizh|kollywood)\\b"),
        LANG_TELUGU to Regex("(?i)\\b(telugu|tollywood)\\b"),
        LANG_MALAYALAM to Regex("(?i)\\b(malayalam|mollywood)\\b"),
        LANG_KANNADA to Regex("(?i)\\b(kannada|sandalwood)\\b"),
        LANG_HINDI to Regex("(?i)\\b(hindi|bollywood)\\b"),
        LANG_PUNJABI to Regex("(?i)\\b(punjabi|pollywood)\\b"),
        LANG_BENGALI to Regex("(?i)\\b(bengali|bangla)\\b"),
        LANG_KOREAN to Regex("(?i)\\b(kpop|k-pop|korean|ost)\\b"),
        LANG_JAPANESE to Regex("(?i)\\b(jpop|j-pop|japanese|anime|opening|ending)\\b"),
        LANG_SPANISH to Regex("(?i)\\b(spanish|español|latino|reggaeton)\\b"),
        LANG_ENGLISH to Regex("(?i)\\b(english|hollywood|billboard|uk pop|us pop)\\b"),
    )

    // Prominent regional artists commonly transliterated in Latin alphabet
    private val TAMIL_ARTISTS = setOf(
        "anirudh ravichander", "anirudh", "a.r. rahman", "ar rahman", "yuvan shankar raja", "yuvan",
        "harris jayaraj", "ilayaraja", "ilaiyaraaja", "d. imman", "imman", "santhosh narayanan", "sa na",
        "g.v. prakash kumar", "gv prakash", "sid sriram", "pradeep kumar", "sean roldan", "hiphop tamizha",
        "vijay antony", "dhanush", "s. p. balasubrahmanyam", "s.p. balasubrahmanyam", "spb", "k. j. yesudas",
        "k.j. yesudas", "hariharan", "unni menon", "karthik", "haricharan", "naresh iyer", "sam c.s.",
        "sam c. s.", "sam cs", "nivas k prasanna", "dhibu ninan thomas", "leon james", "ghibran", "vidyasagar",
        "deva", "stephen zechariah", "sivakarthikeyan", "shakthisree gopalan", "jonita gandhi", "andrea jeremiah",
        "chinmayi", "chinmayi sripada", "arivu", "dhee", "vijay", "silambarasan tr", "str", "kamal haasan",
        "asal kolaar", "paal dabba", "ofro", "sai abhyankkar", "sathyan mahalingam", "anthony daasan",
        "velmurugan", "chinna ponnu", "swetha mohan", "shweta mohan", "saindhavi", "sujatha mohan",
        "s. janaki", "p. susheela", "chithra", "k. s. chithra", "ks chithra", "anuradha sriram", "tippu",
        "krish", "ranjith", "harini", "justin prabhakaran", "vishal chandrashekhar", "darbuka siva", "govind vasantha"
    )

    private val TELUGU_ARTISTS = setOf(
        "devi sri prasad", "dsp", "thaman s", "s. thaman", "thaman", "m. m. keeravaani", "m.m. keeravani",
        "keeravaani", "ram miriyala", "anurag kulkarni", "mangli", "anudeep dev", "kaala bhairava",
        "chaitan bharadwaj", "mahathi swara sagar", "vivek sagar", "radhan", "hema chandra", "geetha madhuri",
        "sunitha", "mickey j meyer", "bheems ceciroleo", "sri krishna", "karunya", "deepu", "revanth",
        "sreerama chandra", "lipsika", "ramya behara", "sahithi", "roll rida"
    )

    private val HINDI_ARTISTS = setOf(
        "arijit singh", "arijit", "pritam", "vishal-shekhar", "vishal dadlani", "shekhar ravjiani", "sachin-jigar",
        "neha kakkar", "shreya ghoshal", "badshah", "atif aslam", "alka yagnik", "kumar sanu",
        "udit narayan", "sonu nigam", "sunidhi chauhan", "jubin nautiyal", "b praak", "tanishk bagchi",
        "mithoon", "amit trivedi", "armaan malik", "amaal mallik", "darshan raval", "yo yo honey singh",
        "honey singh", "meet bros", "mohit chauhan", "kk", "k.k.", "shankar-ehsaan-loy", "shankar mahadevan",
        "shilpa rao", "jasleen royal", "stebin ben", "anuv jain", "zaeden", "prateek kuhad", "king", "mc stan",
        "emiway bantai", "raftaar", "divine"
    )

    private val MALAYALAM_ARTISTS = setOf(
        "sushin shyam", "hesham abdul wahab", "jakes bejoy", "shaan rahman", "gopi sundar",
        "deepak dev", "vineeth sreenivasan", "job kurian", "ks harisankar", "k.s. harisankar",
        "m. jayachandran", "bijibal", "rex vijayan", "alphons joseph", "vidhu prathap", "dabzee", "hanumankind", "fejo"
    )

    private val PUNJABI_ARTISTS = setOf(
        "diljit dosanjh", "ap dhillon", "karan aujla", "sidhu moose wala", "sidhu moosewala",
        "shubh", "guru randhawa", "b praak", "jassie gill", "harrdy sandhu", "amrit maan",
        "parmish verma", "ammy virk", "mankirt aulakh", "jass manak", "gurinder gill"
    )

    private val KOREAN_ARTISTS = setOf(
        "bts", "blackpink", "newjeans", "stray kids", "twice", "iu", "seventeen", "aespa",
        "le sserafim", "exo", "tomorrow x together", "txt", "enhypen", "itzy", "nct", "red velvet"
    )

    private val JAPANESE_ARTISTS = setOf(
        "yoasobi", "kenshi yonezu", "ado", "eve", "official hige dandism", "king gnu", "lisa",
        "radwimps", "aimyon", "vaundy", "fujii kaze", "milet", "yuuri"
    )

    // Prominent Western/English pop, hip-hop, and rock artists
    private val ENGLISH_ARTISTS = setOf(
        "taylor swift", "ed sheeran", "the weeknd", "justin bieber", "ariana grande", "drake",
        "post malone", "billie eilish", "dua lipa", "bruno mars", "eminem", "coldplay",
        "imagine dragons", "maroon 5", "adele", "beyoncé", "beyonce", "rihanna", "katy perry",
        "shawn mendes", "harry styles", "olivia rodrigo", "kendrick lamar", "travis scott",
        "snoop dogg", "kanye west", "selena gomez", "sam smith", "charlie puth", "david guetta",
        "calvin harris", "marshmello", "alan walker", "lady gaga", "sia", "zayn", "camila cabello",
        "cardi b", "doja cat", "sabrina carpenter", "chappell roan", "teddy swims", "benson boone",
        "morgan wallen", "luke combs", "jack harlow", "lil nas x", "hozier", "mariah carey",
        "miley cyrus", "shakira", "pitbull", "avicii", "chainsmokers", "the chainsmokers",
        "one direction", "linkin park", "queen", "michael jackson", "madonna", "the beatles",
        "twenty one pilots", "onerepublic", "one republic", "glass animals", "conan gray",
        "alec benjamin", "lauv", "troye sivan", "lizzo", "bebe rexha", "anne-marie", "clean bandit",
        "rita ora", "ellie goulding", "jessie j", "demi lovato", "kesha", "pink", "kelly clarkson",
        "avril lavigne", "britney spears", "christina aguilera", "celine dion", "whitney houston",
        "alicia keys", "john legend", "jason derulo", "chris brown", "usher", "ne-yo", "neyo",
        "akon", "flo rida", "50 cent", "jay-z", "jay z", "lil wayne", "future", "21 savage",
        "metro boomin", "gunna", "young thug", "lil baby", "dababy", "roddy ricch", "polo g",
        "juice wrld", "xxxtentacion", "mac miller", "tyler, the creator", "asap rocky", "playboi carti",
        "central cee", "cigarettes after sex", "arctic monkeys", "the neighbourhood", "tame impala",
        "nirvana", "metallica", "green day", "radiohead"
    )

    /**
     * Extracts the dominant language from a collection of tracks.
     */
    fun getDominantLanguage(tracks: List<Track>): String {
        val detected = tracks.map { detectLanguage(it) }
            .filter { it != LANG_UNKNOWN }
        if (detected.isEmpty()) return LANG_UNKNOWN
        return detected.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: LANG_UNKNOWN
    }

    /**
     * Detects the language of a [Track] using Unicode scripts, title/album keywords, and prominent artists.
     */
    fun detectLanguage(track: Track): String {
        val title = track.title
        val album = track.album?.name.orEmpty()
        val artistNames = track.artists?.map { it.name.trim().lowercase() }.orEmpty()
        val combinedText = "$title $album ${artistNames.joinToString(" ")}"

        // 1. Unicode Script Check (highest confidence)
        for (char in combinedText) {
            for ((lang, range) in SCRIPT_RANGES) {
                if (char in range) {
                    return lang
                }
            }
        }

        // 2. Explicit Keyword Matches
        for ((lang, regex) in KEYWORD_PATTERNS) {
            if (regex.containsMatchIn(combinedText)) {
                return lang
            }
        }

        // 3. Known Prominent Regional Artists
        for (artist in artistNames) {
            when {
                TAMIL_ARTISTS.any { artist.contains(it) || it.contains(artist) } -> return LANG_TAMIL
                TELUGU_ARTISTS.any { artist.contains(it) || it.contains(artist) } -> return LANG_TELUGU
                HINDI_ARTISTS.any { artist.contains(it) || it.contains(artist) } -> return LANG_HINDI
                MALAYALAM_ARTISTS.any { artist.contains(it) || it.contains(artist) } -> return LANG_MALAYALAM
                PUNJABI_ARTISTS.any { artist.contains(it) || it.contains(artist) } -> return LANG_PUNJABI
                KOREAN_ARTISTS.any { artist.contains(it) || it.contains(artist) } -> return LANG_KOREAN
                JAPANESE_ARTISTS.any { artist.contains(it) || it.contains(artist) } -> return LANG_JAPANESE
                ENGLISH_ARTISTS.any { artist.contains(it) || it.contains(artist) } -> return LANG_ENGLISH
            }
        }

        return LANG_UNKNOWN
    }

    /**
     * Determines whether a candidate [Track] is compatible with the detected language of reference tracks.
     * When listening to regional songs (Tamil, Telugu, Hindi, etc.), non-matching languages and English songs
     * are strictly rejected to preserve the user's language immersion during playback and skipping.
     */
    fun isCompatibleLanguage(candidate: Track, referenceTracks: List<Track>): Boolean {
        if (referenceTracks.isEmpty()) return true

        // Rule 1: Artist continuity. If candidate shares any artist with reference tracks, keep it!
        val referenceArtists = referenceTracks.flatMap { it.artists.orEmpty() }
            .map { it.name.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()

        val candidateArtists = candidate.artists.orEmpty()
            .map { it.name.trim().lowercase() }
            .filter { it.isNotBlank() }

        if (candidateArtists.any { it in referenceArtists }) {
            return true
        }

        // Rule 2: Dominant reference language
        val dominantLang = getDominantLanguage(referenceTracks)
        if (dominantLang == LANG_UNKNOWN) {
            return true
        }

        val candidateLang = detectLanguage(candidate)

        // Rule 3: Direct language match
        if (candidateLang == dominantLang) {
            return true
        }

        // Rule 4: If dominant language is regional (Tamil, Telugu, Hindi, Malayalam, Kannada, Punjabi, etc.)
        // Strictly reject English songs and candidate tracks without matching regional signals!
        val isRegional = dominantLang in setOf(
            LANG_TAMIL, LANG_TELUGU, LANG_HINDI, LANG_MALAYALAM, LANG_KANNADA, LANG_PUNJABI, LANG_BENGALI
        )

        if (isRegional) {
            if (candidateLang == LANG_ENGLISH || candidateLang != dominantLang) {
                Logger.d(TAG, "Rejected candidate '${candidate.title}' ($candidateLang) incompatible with regional language ($dominantLang)")
                return false
            }
        }

        // Rule 5: If candidate has a distinct known language that conflicts with dominantLang, reject it
        if (candidateLang != LANG_UNKNOWN && candidateLang != dominantLang) {
            Logger.d(TAG, "Rejected track '${candidate.title}' ($candidateLang) due to language mismatch with queue ($dominantLang)")
            return false
        }

        return true
    }
}
