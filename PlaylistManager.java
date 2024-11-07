import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javazoom.jl.player.Player;

class SongNode {
    String songPath;
    SongNode next, prev;

    public SongNode(String songPath) {
        this.songPath = songPath;
        this.next = null;
        this.prev = null;
    }
}

class PlaylistManager extends Frame implements ActionListener {
    public List playlist;
    public Button uploadButton, shuffle, sortButton, playButton, pauseButton, countButton, removeButton, nextButton, prevButton;
    public Canvas timelineCanvas;
    public Label durationLabel;
    public ArrayList<SongNode> songList;
    public SongNode currentSong = null;
    public Player player;
    public Thread playerThread;
    public boolean isPaused = false;
    public FileInputStream currentSongStream;
    public int songLength = 0;
    public int playPosition = 0;
    public int totalDurationInSeconds = 0;

    public PlaylistManager() {
        super("Playlist Manager");
        setSize(800, 500);
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 220));

        songList = new ArrayList<>();
        playlist = new List(10);

        uploadButton = new Button("Upload MP3");
        shuffle = new Button("Shuffle Songs");
        sortButton = new Button("Sort Songs");
        playButton = new Button("Play Song");
        pauseButton = new Button("Pause");
        countButton = new Button("Count Songs");
        nextButton = new Button("Next");
        prevButton = new Button("Previous");
        removeButton = new Button("Remove Song");

        uploadButton.setBackground(Color.BLUE);
        shuffle.setBackground(Color.LIGHT_GRAY);
        sortButton.setBackground(Color.LIGHT_GRAY);
        playButton.setBackground(Color.GRAY);
        pauseButton.setBackground(Color.GRAY);
        countButton.setBackground(Color.GREEN);
        removeButton.setBackground(Color.RED);
        nextButton.setBackground(Color.GRAY);
        prevButton.setBackground(Color.GRAY);

        uploadButton.setForeground(Color.WHITE);
        shuffle.setForeground(Color.WHITE);
        sortButton.setForeground(Color.WHITE);
        playButton.setForeground(Color.WHITE);
        pauseButton.setForeground(Color.WHITE);
        countButton.setForeground(Color.WHITE);
        removeButton.setForeground(Color.WHITE);
        nextButton.setForeground(Color.WHITE);
        prevButton.setForeground(Color.WHITE);

        Panel buttonPanel = new Panel();
        buttonPanel.setLayout(new GridLayout(0, 1));

        uploadButton.addActionListener(this);
        shuffle.addActionListener(this);
        sortButton.addActionListener(this);
        playButton.addActionListener(this);
        pauseButton.addActionListener(this);
        countButton.addActionListener(this);
        removeButton.addActionListener(this);
        nextButton.addActionListener(this);
        prevButton.addActionListener(this);

        buttonPanel.add(uploadButton);
        buttonPanel.add(shuffle);
        buttonPanel.add(sortButton);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(countButton);
        buttonPanel.add(prevButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(removeButton);

        add(buttonPanel, BorderLayout.WEST);
        add(playlist, BorderLayout.CENTER);

        timelineCanvas = new Canvas() {
            @Override
            public void paint(Graphics g) {
                int width = getWidth();
                int height = getHeight();
                g.setColor(Color.LIGHT_GRAY);
                g.fillRect(0, 0, width, height);
                g.setColor(Color.BLUE);
                int filledWidth = (int) ((playPosition / (float) songLength) * width);
                g.fillRect(0, 0, filledWidth, height);
            }
        };
        timelineCanvas.setSize(700, 20);
        add(timelineCanvas, BorderLayout.SOUTH);

        durationLabel = new Label("00:00 / 00:00");
        add(durationLabel, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                stopPlayer();
                System.exit(0);
            }
        });

        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == uploadButton) {
            FileDialog fd = new FileDialog(this, "Select MP3 file", FileDialog.LOAD);
            fd.setFile("*.mp3");
            fd.setVisible(true);

            String directory = fd.getDirectory();
            String filename = fd.getFile();
            if (filename != null) {
                String songPath = directory + filename;
                songList.add(new SongNode(songPath));
                updatePlaylist();
            }
        } else if (e.getSource() == shuffle) {
            Collections.shuffle(songList);
            updatePlaylist();
        } else if (e.getSource() == sortButton) {
            songList.sort(Comparator.comparing(s -> new File(s.songPath).getName()));
            updatePlaylist();
        } else if (e.getSource() == playButton) {
            if (currentSong == null && !songList.isEmpty()) {
                playSong(songList.get(0));
            } else if (currentSong != null) {
                playSong(currentSong);
            }
        } else if (e.getSource() == pauseButton) {
            togglePause();
        } else if (e.getSource() == countButton) {
            showSongCount();
        } else if (e.getSource() == removeButton) {
            removeSong();
        } else if (e.getSource() == nextButton) {
            nextSong();
        } else if (e.getSource() == prevButton) {
            prevSong();
        }
    }

    private void updatePlaylist() {
        playlist.removeAll();
        for (SongNode song : songList) {
            playlist.add(new File(song.songPath).getName().replace(".mp3", ""));
        }
    }

    private void playSong(SongNode song) {
        if (song != null) {
            stopPlayer();
            currentSong = song;
            try {
                currentSongStream = new FileInputStream(song.songPath);
                currentSongStream.skip(playPosition);  // Resume from last position
                songLength = currentSongStream.available();
                totalDurationInSeconds = (songLength + playPosition) / 20000;
                player = new Player(currentSongStream);
                playerThread = new Thread(() -> {
                    try {
                        while (playPosition < songLength && player.play(1)) {
                            playPosition = songLength - currentSongStream.available();
                            updateTimeline();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                playerThread.start();
                isPaused = false;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void togglePause() {
        if (player != null) {
            if (!isPaused) {
                try {
                    playPosition = songLength - currentSongStream.available();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                stopPlayer();
                isPaused = true;
                pauseButton.setLabel("Resume");
            } else {
                playSong(currentSong);  // Resume playback from last saved position
                isPaused = false;
                pauseButton.setLabel("Pause");
            }
        }
    }

    private void showSongCount() {
        Dialog dialog = new Dialog(this, "Song Count", true);
        dialog.setLayout(new FlowLayout());
        dialog.add(new Label("Total Songs: " + songList.size()));
        Button okButton = new Button("OK");
        okButton.addActionListener(ae -> dialog.setVisible(false));
        dialog.add(okButton);
        dialog.setSize(200, 100);
        dialog.setVisible(true);
    }

    private void stopPlayer() {
        if (player != null) {
            player.close();
        }
        if (playerThread != null && playerThread.isAlive()) {
            playerThread.interrupt();
        }
    }

    private void updateTimeline() {
        timelineCanvas.repaint();
        int currentSeconds = playPosition / 20000;
        String currentTime = String.format("%02d:%02d", currentSeconds / 60, currentSeconds % 60);
        String totalTime = String.format("%02d:%02d", totalDurationInSeconds / 60, totalDurationInSeconds % 60);
        durationLabel.setText(currentTime + " / " + totalTime);
    }

    private void removeSong() {
        if (currentSong != null) {
            stopPlayer();
            songList.remove(currentSong);
            updatePlaylist();
            currentSong = songList.isEmpty() ? null : songList.get(0);
            playPosition = 0;
        }
    }

    private void nextSong() {
        if (currentSong != null && songList.indexOf(currentSong) < songList.size() - 1) {
            playSong(songList.get(songList.indexOf(currentSong) + 1));
        }
    }

    private void prevSong() {
        if (currentSong != null && songList.indexOf(currentSong) > 0) {
            playSong(songList.get(songList.indexOf(currentSong) - 1));
        }
    }

    public static void main(String[] args) {
        new PlaylistManager();
    }
}