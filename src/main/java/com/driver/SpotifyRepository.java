package com.driver;

import java.util.*;

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

    public List<User> users;
    public List<Song> songs;
    public List<Playlist> playlists;
    public List<Album> albums;
    public List<Artist> artists;

    public SpotifyRepository(){
        //To avoid hitting apis multiple times, initialize all the hashmaps here with some dummy data
        artistAlbumMap = new HashMap<>();
        albumSongMap = new HashMap<>();
        playlistSongMap = new HashMap<>();
        playlistListenerMap = new HashMap<>();
        creatorPlaylistMap = new HashMap<>();
        userPlaylistMap = new HashMap<>();
        songLikeMap = new HashMap<>();

        users = new ArrayList<>();
        songs = new ArrayList<>();
        playlists = new ArrayList<>();
        albums = new ArrayList<>();
        artists = new ArrayList<>();
    }

    public User createUser(String name, String mobile) {
        User user=new User(name,mobile);
        users.add(user);

        userPlaylistMap.put(user,new ArrayList<>());
        return user;
    }

    public Artist createArtist(String name) {
        Artist artist=new Artist(name);
        artists.add(artist);
        artistAlbumMap.put(artist,new ArrayList<>());

        return artist;
    }

    public Album createAlbum(String title, String artistName) {
        Artist artist=null;
        for(Artist artist1:artists){
            if(artist1.getName().equals(artistName)){
                artist=artist1;
                break;
            }
        }
        if(artist == null)
            artist=createArtist(artistName);

        Album album=new Album(title);
        albums.add(album);

        artistAlbumMap.get(artist).add(album);
        albumSongMap.put(album,new ArrayList<>());

        return album;

    }

    public Song createSong(String title, String albumName, int length) throws Exception{
        Album album=null;
        for(Album album1:albums){
            if(album1.getTitle().equals(albumName)){
                album=album1;
                break;
            }
        }
        if(album == null){
            throw new Exception("Album does not exist");
        }
        Song song=new Song(title,length);
        song.setLikes(0);
        songs.add(song);

        albumSongMap.get(album).add(song);

        songLikeMap.put(song,new ArrayList<>());

        return song;

    }

    public Playlist createPlaylistOnLength(String mobile, String title, int length) throws Exception {
        User user=null;
        for(User user1:users){
            if(user1.getMobile().equals(mobile)){
                user=user1;
                break;
            }
        }
        if(user == null)
            throw new Exception("User does not exist");

        Playlist playlist=new Playlist(title);
        playlists.add(playlist);

        playlistSongMap.put(playlist,new ArrayList<>());
        playlistListenerMap.put(playlist,new ArrayList<>());
        //  userPlaylistMap.put(user,new ArrayList<>());

        for(Song song:songs){
            if(song.getLength() == length)
                playlistSongMap.get(playlist).add(song);
        }

        playlistListenerMap.get(playlist).add(user);   //current listener of the playlist
        creatorPlaylistMap.put(user,playlist);         //creator of the playlist
        userPlaylistMap.get(user).add(playlist);   //user and his list of playlist


        return playlist;

    }

    public Playlist createPlaylistOnName(String mobile, String title, List<String> songTitles) throws Exception {
        User user=null;
        for(User user1:users){
            if(user1.getMobile().equals(mobile)){
                user=user1;
                break;
            }
        }
        if(user == null)
            throw new Exception("User does not exist");

        Playlist playlist=new Playlist(title);
        playlists.add(playlist);

        playlistSongMap.put(playlist,new ArrayList<>());
        playlistListenerMap.put(playlist,new ArrayList<>());
        // userPlaylistMap.put(user,new ArrayList<>());

        for(Song song:songs){
            if(songTitles.contains(song.getTitle()))
                playlistSongMap.get(playlist).add(song);
        }

        playlistListenerMap.get(playlist).add(user);   //current listener of the playlist
        creatorPlaylistMap.put(user,playlist);         //creator of the playlist
        userPlaylistMap.get(user).add(playlist);   //user and his list of playlist


        return playlist;
    }

    public Playlist findPlaylist(String mobile, String playlistTitle) throws Exception {
        User user=null;
        for(User user1:users){
            if(user1.getMobile().equals(mobile)){
                user=user1;
                break;
            }
        }
        if(user == null)
            throw new Exception("User does not exist");

        Playlist playlist=null;
        for(Playlist playlist1:playlists){
            if(playlist1.getTitle().equals(playlistTitle)){
                playlist=playlist1;
                break;
            }
        }
        if(playlist == null)
            throw new Exception("Playlist does not exist");

        if(creatorPlaylistMap.containsKey(user) && creatorPlaylistMap.get(user) == playlist ||
                playlistListenerMap.get(playlist).contains(user)){

            return playlist;
        }
        playlistListenerMap.get(playlist).add(user);

        if(!userPlaylistMap.get(user).contains(playlist)){
            userPlaylistMap.get(user).add(playlist);
        }


        return playlist;
    }

    public Song likeSong(String mobile, String songTitle) throws Exception {
        User user=null;
        for(User user1:users){
            if(user1.getMobile().equals(mobile)){
                user=user1;
                break;
            }
        }
        if(user == null)
            throw new Exception("User does not exist");

        Song song=null;
        for(Song song1:songs){
            if(song1.getTitle().equals(songTitle)){
                song=song1;
                break;
            }
        }
        if(song == null)
            throw new Exception("Song does not exist");

        if(songLikeMap.get(song).contains(user)){
            return song;
        }
        song.setLikes(song.getLikes()+1);
        songLikeMap.get(song).add(user);

        for(Album album:albumSongMap.keySet()){
            if(albumSongMap.get(album).contains(song)){
                for(Artist artist:artistAlbumMap.keySet()){
                    if(artistAlbumMap.get(artist).contains(album)){
                        artist.setLikes(artist.getLikes()+1);
                        break;
                    }
                }
                break;
            }
        }
        return song;
    }

    public String mostPopularArtist() {
        int countLikes=Integer.MIN_VALUE;
        String popularArtist="";
        for(Artist artist:artists){
            if(artist.getLikes() > countLikes){
                popularArtist=artist.getName();
                countLikes=artist.getLikes();
            }
        }
        return popularArtist;
    }

    public String mostPopularSong() {
        int countLikes=Integer.MIN_VALUE;
        String popularSong="";
        for(Song song:songs){
            if(song.getLikes() > countLikes){
                popularSong=song.getTitle();
                countLikes=song.getLikes();
            }
        }
        return popularSong;
    }
}