package com.example.pickup.data.local

// Starting content, inserted once when the database is first created.
// The ids are fixed here so each song can point at its tuning.
object SeedData {
    val tunings = listOf(
        TuningEntity(1, "Standard", "E2 A2 D3 G3 B3 E4", "E minor pentatonic;G major", "G D Em C;Am F C G"),
        TuningEntity(2, "Drop D", "D2 A2 D3 G3 B3 E4", "D minor pentatonic;D major", "D G A;Dm Bb C"),
        TuningEntity(3, "DADGAD", "D2 A2 D3 G3 A3 D4", "D mixolydian;D major", "D G A;D C G"),
        TuningEntity(4, "Open G", "D2 G2 D3 G3 B3 D4", "G major pentatonic;G mixolydian", "G C D;G F C"),
        TuningEntity(5, "Open D", "D2 A2 D3 F#3 A3 D4", "D major pentatonic;D mixolydian", "D G A;D A G"),
        TuningEntity(6, "Half Step Down", "Eb2 Ab2 Db3 Gb3 Bb3 Eb4", "Eb minor pentatonic;Gb major", "Gb Db Ebm B;Ebm B Gb Db"),
        TuningEntity(7, "Double Drop D", "D2 A2 D3 G3 B3 D4", "D minor pentatonic;G major", "D C G;G D C")
    )

    val songs = listOf(
        SongEntity(1, "Blackbird", "The Beatles", 1),
        SongEntity(2, "Wish You Were Here", "Pink Floyd", 1),
        SongEntity(3, "Everlong", "Foo Fighters", 2),
        SongEntity(4, "Killing in the Name", "Rage Against the Machine", 2),
        SongEntity(5, "Black Mountain Side", "Led Zeppelin", 3),
        SongEntity(6, "Kashmir", "Led Zeppelin", 3),
        SongEntity(7, "Brown Sugar", "The Rolling Stones", 4),
        SongEntity(8, "Start Me Up", "The Rolling Stones", 4),
        SongEntity(9, "Even Flow", "Pearl Jam", 5),
        SongEntity(10, "Little Wing", "Jimi Hendrix", 6),
        SongEntity(11, "Sweet Child O' Mine", "Guns N' Roses", 6),
        SongEntity(12, "Cinnamon Girl", "Neil Young", 7),
        SongEntity(13, "Going to California", "Led Zeppelin", 7)
    )
}
