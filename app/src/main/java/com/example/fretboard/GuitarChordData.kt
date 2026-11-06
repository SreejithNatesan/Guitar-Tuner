package com.example.fretboard

object GuitarChordData {

  val BEGINNER_CHORDS = listOf(
    GuitarChord(
      name = "C Major",
      symbol = "C",
      voicingPattern = "x32010",
      description = "One of the most popular guitar chords in music. Creates a bright, happy sound.",
      tips = "Keep your thumb behind the neck and arch your fingers so the open G and high E strings ring cleanly.",
      positions = listOf(
        ChordPosition(6, null, null, null), // muted (x)
        ChordPosition(5, 3, 3, "C"),       // fret 3, Ring finger
        ChordPosition(4, 2, 2, "E"),       // fret 2, Middle finger
        ChordPosition(3, 0, null, "G"),    // open string (0)
        ChordPosition(2, 1, 1, "C"),       // fret 1, Index finger
        ChordPosition(1, 0, null, "E")     // open string (0)
      )
    ),

    GuitarChord(
      name = "G Major",
      symbol = "G",
      voicingPattern = "320003",
      description = "Full, resonant chord that uses all six strings. Found in hundreds of classic songs.",
      tips = "Use your middle finger on the low 6th string and index on the 5th string so switching to C is easy.",
      positions = listOf(
        ChordPosition(6, 3, 2, "G"),       // fret 3, Middle finger
        ChordPosition(5, 2, 1, "B"),       // fret 2, Index finger
        ChordPosition(4, 0, null, "D"),    // open string
        ChordPosition(3, 0, null, "G"),    // open string
        ChordPosition(2, 0, null, "B"),    // open string
        ChordPosition(1, 3, 3, "G")        // fret 3, Ring finger
      )
    ),

    GuitarChord(
      name = "D Major",
      symbol = "D",
      voicingPattern = "xx0232",
      description = "Crisp, triumphant triangle-shaped chord played across the top four strings.",
      tips = "Mute or avoid strumming strings 6 and 5. Focus your strum starting from the open 4th (D) string.",
      positions = listOf(
        ChordPosition(6, null, null, null), // muted (x)
        ChordPosition(5, null, null, null), // muted (x)
        ChordPosition(4, 0, null, "D"),     // open 4th string
        ChordPosition(3, 2, 1, "A"),        // fret 2, Index finger
        ChordPosition(2, 3, 3, "D"),        // fret 3, Ring finger
        ChordPosition(1, 2, 2, "F#")        // fret 2, Middle finger
      )
    ),

    GuitarChord(
      name = "A Minor",
      symbol = "Am",
      voicingPattern = "x02210",
      description = "Warm, emotional minor chord. Shares the exact same finger shape as E Major shifted down one string.",
      tips = "Keep your 1st finger arched on the 2nd string so the high 1st string rings open and clear.",
      positions = listOf(
        ChordPosition(6, null, null, null), // muted (x)
        ChordPosition(5, 0, null, "A"),     // open 5th string
        ChordPosition(4, 2, 2, "E"),        // fret 2, Middle finger
        ChordPosition(3, 2, 3, "A"),        // fret 2, Ring finger
        ChordPosition(2, 1, 1, "C"),        // fret 1, Index finger
        ChordPosition(1, 0, null, "E")      // open 1st string
      )
    ),

    GuitarChord(
      name = "E Minor",
      symbol = "Em",
      voicingPattern = "022000",
      description = "The easiest guitar chord to learn! Rich, deep, and melancholic using all 6 strings.",
      tips = "Only two fingers needed! Place middle and ring fingers right next to the 2nd fret bar.",
      positions = listOf(
        ChordPosition(6, 0, null, "E"),     // open 6th string
        ChordPosition(5, 2, 2, "B"),        // fret 2, Middle finger
        ChordPosition(4, 2, 3, "E"),        // fret 2, Ring finger
        ChordPosition(3, 0, null, "G"),     // open string
        ChordPosition(2, 0, null, "B"),     // open string
        ChordPosition(1, 0, null, "E")      // open string
      )
    ),

    GuitarChord(
      name = "F Major",
      symbol = "F",
      voicingPattern = "xx3211",
      description = "Essential chord found in thousands of songs. Can be played using the easy 4-string beginner shape.",
      tips = "Flatten your 1st finger slightly across strings 1 and 2 at fret 1. Strum from the 4th string down.",
      positions = listOf(
        ChordPosition(6, null, null, null), // muted (x)
        ChordPosition(5, null, null, null), // muted (x)
        ChordPosition(4, 3, 3, "F"),        // fret 3, Ring finger
        ChordPosition(3, 2, 2, "A"),        // fret 2, Middle finger
        ChordPosition(2, 1, 1, "C"),        // fret 1, Index finger
        ChordPosition(1, 1, 1, "F")         // fret 1, Index finger
      )
    )
  )
}
