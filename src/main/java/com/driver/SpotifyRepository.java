package com.driver;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

@Repository
public class SpotifyRepository {
    public HashMap<Artist, List<Album>> artistAlbumMap;
    public HashMap<Album, List<Song>> albumSongMap;
    public HashMap<Playlist, List<Song>> playlistSongMap;
    public HashMap<Playlist, List<User>> playlistListenerMap;
    public HashMap<User, Playlist> creatorPlaylistMap;
    public HashMap<User, List<Playlist>> userPlaylistMap;
    public HashMap<Song, List<User>> songLikeMap;

    public Map<String, List<User>> artistLikeMap;

    public List<User> users;
    public List<Song> songs;
    public List<Playlist> playlists;
    public List<Album> albums;
    public List<Artist> artists;

    public SpotifyRepository() throws Exception {
        //To avoid hitting apis multiple times, initialize all the hashmaps here with some dummy data
        artistAlbumMap = new HashMap<>();
        albumSongMap = new HashMap<>();
        playlistSongMap = new HashMap<>();
        playlistListenerMap = new HashMap<>();
        creatorPlaylistMap = new HashMap<>();
        userPlaylistMap = new HashMap<>();
        songLikeMap = new HashMap<>();
        artistLikeMap = new HashMap<>();

        users = new ArrayList<>();
        songs = new ArrayList<>();
        playlists = new ArrayList<>();
        albums = new ArrayList<>();
        artists = new ArrayList<>();

        // Initialize some dummy data for testing
        User user1 = createUser("John Doe", "1234567890");
        User user2 = createUser("Alice Smith", "9876543210");
        users.addAll(Arrays.asList(user1, user2));

        Artist artist1 = createArtist("Artist1");
        Artist artist2 = createArtist("Artist2");
        artists.addAll(Arrays.asList(artist1, artist2));

        Album album1 = createAlbum("Album1", "Artist1");
        Album album2 = createAlbum("Album2", "Artist2");
        albums.addAll(Arrays.asList(album1, album2));

        Song song1 = createSong("Song1", "Album1", 200);
        Song song2 = createSong("Song2", "Album2", 180);
        songs.addAll(Arrays.asList(song1, song2));

        Playlist playlist1 = createPlaylistOnLength("1234567890", "My Playlist", 200);
        playlists.add(playlist1);
    }

    public User createUser(String name, String mobile) {
        User user = new User(name, mobile);
        users.add(user);
        return user;
    }

    public Artist createArtist(String name) {
        Artist artist = new Artist(name);
        artists.add(artist);
        return artist;
    }

    public Album createAlbum(String title, String artistName) {
        Artist artist = findArtistByName(artistName);

        if (artist == null) {
            try {
                artist = createArtist(artistName);
            } catch (Exception e) {
                // Handle the exception, e.g., log it or throw a more specific exception
                throw new RuntimeException("Failed to create artist: " + e.getMessage());
            }
        }

        Album album = new Album(title);

        List<Album> albumsByArtist = artistAlbumMap.computeIfAbsent(artist, k -> new ArrayList<>());
        albumsByArtist.add(album);
        albums.add(album);

        return album;
    }

    public Song createSong(String title, String albumName, int length) throws Exception{
        Album album = findAlbumByTitle(albumName);

        if (album == null) {
            throw new Exception("Album does not exist");
        }

        // Assuming Song class has a constructor that takes a title and length
        Song song = new Song(title, length);

        List<Song> songsInAlbum = albumSongMap.computeIfAbsent(album, k -> new ArrayList<>());
        songsInAlbum.add(song);
        songs.add(song);

        return song;
    }

    public Playlist createPlaylistOnLength(String mobile, String title, int length) throws Exception {
        User user = findUserByMobile(mobile);
        if (user == null) {
            throw new Exception("User does not exist");
        }

        Playlist playlist = new Playlist(title);
        List<Song> songsWithGivenLength = findSongsByLength(length);
        playlistSongMap.put(playlist, songsWithGivenLength);
        playlistListenerMap.computeIfAbsent(playlist, k -> new ArrayList<>()).add(user);
        creatorPlaylistMap.put(user, playlist);
        userPlaylistMap.computeIfAbsent(user, k -> new ArrayList<>()).add(playlist);
        playlists.add(playlist);

        return playlist;
    }

    public Playlist createPlaylistOnName(String mobile, String title, List<String> songTitles) throws Exception {
        User user = findUserByMobile(mobile);

        if (user == null) {
            throw new Exception("User does not exist");
        }

        Playlist playlist = new Playlist(title);

        // Find songs with the given titles
        List<Song> songsWithGivenTitles = findSongsByTitles(songTitles);

        // Add songs to the playlist
        playlistSongMap.put(playlist, songsWithGivenTitles);

        // The user is the only listener at the time of playlist creation
        playlistListenerMap.computeIfAbsent(playlist, k -> new ArrayList<>()).add(user);

        // Set the user as the creator of the playlist
        creatorPlaylistMap.put(user, playlist);

        // Add the playlist to the user's playlist map
        userPlaylistMap.computeIfAbsent(user, k -> new ArrayList<>()).add(playlist);

        // Add the playlist to the global list of playlists
        playlists.add(playlist);

        return playlist;
    }

    public Playlist findPlaylist(String mobile, String playlistTitle) throws Exception {
        User user = findUserByMobile(mobile);
        if (user == null) {
            throw new Exception("User does not exist");
        }

        Playlist playlist = playlists.stream()
                .filter(p -> p.getTitle().equals(playlistTitle))
                .findFirst()
                .orElseThrow(() -> new Exception("Playlist does not exist"));

        if (!playlistListenerMap.containsKey(playlist) || !playlistListenerMap.get(playlist).contains(user)) {
            playlistListenerMap.computeIfAbsent(playlist, k -> new ArrayList<>()).add(user);
            userPlaylistMap.computeIfAbsent(user, k -> new ArrayList<>()).add(playlist);
        }

        return playlist;
    }

    public Song likeSong(String mobile, String songTitle) throws Exception {
        User user = findUserByMobile(mobile);

        if (user == null) {
            throw new Exception("User does not exist");
        }

        Song song = findSongByTitle(songTitle);

        if (!songLikeMap.containsKey(song) || !songLikeMap.get(song).contains(user)) {
            songLikeMap.computeIfAbsent(song, k -> new ArrayList<>()).add(user);

            // Assuming you have a way to get the artist from the song
            Artist artist = findArtistBySong(song);
            if (artist != null) {
                likeArtist(artist, user);
            }
        }

        return song;
    }

    public String mostPopularArtist() {
        Map<Artist, Long> artistLikesCount = songLikeMap.keySet().stream().map(Song::getAlbum).map(Album::getArtist)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return artistLikesCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .map(Artist::getName)
                .orElse(null);
    }

    public String mostPopularSong() {
        Map<Song, Long> songLikesCount = songLikeMap.keySet().stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return songLikesCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .map(Song::getTitle)
                .orElse(null);
    }

    private List<Song> findSongsByLength(int length) {
        return songs.stream()
                .filter(song -> song.getLength() == length)
                .collect(Collectors.toList());
    }

    private List<Song> findSongsByTitles(List<String> songTitles) {
        return songs.stream().filter(song -> songTitles.contains(song.getTitle())).collect(Collectors.toList());
    }

    private User findUserByMobile(String mobile) {
        return users.stream()
                .filter(user -> user.getMobile().equals(mobile))
                .findFirst()
                .orElse(null);
    }

    private Artist findArtistByName(String name) {
        return artists.stream()
                .filter(artist -> artist.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private Album findAlbumByTitle(String title) {
        return albums.stream()
                .filter(album -> album.getTitle().equals(title))
                .findFirst()
                .orElse(null);
    }

    private void likeArtist(Artist artist, User user) {
        String key = artist.getName();  // You may adjust this key based on your requirement
        artistLikeMap.computeIfAbsent(key, k -> new ArrayList<>()).add(user);
    }

    private Song findSongByTitle(String songTitle) throws Exception {
        return songs.stream()
                .filter(s -> s.getTitle().equals(songTitle))
                .findFirst()
                .orElseThrow(() -> new Exception("Song does not exist"));
    }

    private Artist findArtistBySong(Song song) {
        if (song.getAlbum() != null) {
            return song.getAlbum().getArtist();
        }
        return null;
    }

}
