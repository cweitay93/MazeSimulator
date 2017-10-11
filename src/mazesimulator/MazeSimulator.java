/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mazesimulator;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.*;

import map.Constants;
import map.MapUI;

import robot.RobotMap;
import robot.RobotConstant;
import robot.RobotConstant.DIRECTION;
import robot.Robot;

/**
 * Creates a random maze, then solves it by finding a path from the upper left
 * corner to the lower right corner. (After doing one maze, it waits a while
 * then starts over by creating a new random maze.)
 */
public class MazeSimulator extends JPanel {

    // JFrame for the application
    private static JFrame window = null;

    // JPanel for laying out different views
    private static JPanel mainPanel = null;
    private static JPanel buttonsPanel = null;

    private static MapUI mapUI = null;
    private static JPanel mainButtons = null;

    // The robot map used for exploration & shortest path
    private static RobotMap robotMap = null;

    // Robot's starting position and direction
    private static int startPosRow = RobotConstant.DEFAULT_START_ROW;
    private static int startPosCol = RobotConstant.DEFAULT_START_COL;
    private static DIRECTION startDir = RobotConstant.DEFAULT_START_DIR;
    private static DIRECTION spStartDir = RobotConstant.DEFAULT_START_SP_DIR;

    // The robot
    private static robot.Robot roboCop = null;

    // Map width & height used to render real & robot map
    private static int mapWidth;
    private static int mapHeight;

    // File name of the loaded map
    private static String _loadedMapFilename = null;

    // main routine to run this program 
    public static void main(String[] args) {

        // initiate simulated robot
        if (roboCop == null) {
            roboCop = new Robot(startPosRow, startPosCol, startDir);
        }

        // Calculate map width & height based on grid size
        mapWidth = Constants.MAP_COLS * Constants.GRID_SIZE;
        mapHeight = Constants.MAP_ROWS * Constants.GRID_SIZE;

        // Initialise JFrame window
        window = new JFrame("Maze Simulator");
        window.setLocation(120, 80);
        //window.setResizable(false);
        window.setLayout(new GridLayout(1, 2, 3, 3));
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Init Map Panel
        mainPanel = new JPanel(new CardLayout());
        // Init Buttons Panel
        buttonsPanel = new JPanel(new CardLayout());

        mapUI = new MapUI();

        mainPanel.add(mapUI, "MAIN");

        // Initialize the robot map, used for exploration and shortest path
        robotMap = new RobotMap(mapUI);
        mainPanel.add(robotMap, "ROBOT MAP");

        // Show the real map (main menu) by default
        CardLayout cl = ((CardLayout) mainPanel.getLayout());
        cl.show(mainPanel, "MAIN");

        addButtons();

        // Add CardLayouts to content pane
        Container contentPane = window.getContentPane();
        contentPane.add(mainPanel, BorderLayout.WEST);
        contentPane.add(buttonsPanel, BorderLayout.EAST);

//        window.add(mapUI);
//        window.add(buttonsPanel);
//        window.pack();  
        // Display the application  
        window.setSize(new Dimension(920, 648));
        window.setVisible(true);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    }

    public static void addButtons() {
        mainButtons = new JPanel();

        //-----------------------------------------------------------------------------//
        JButton btn_clearObs = new JButton("Clear Obstacles");
        btn_clearObs.setFont(new Font("Arial", Font.BOLD, 18));
        btn_clearObs.setMargin(new Insets(10, 15, 10, 15));
        btn_clearObs.setFocusPainted(false);

        btn_clearObs.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                // Clear the current map
                System.out.println("Clearing Obstacles..");
                mapUI.clearMap();
            }
        });
        mainButtons.add(btn_clearObs);

        JButton btn_loadMap = new JButton("Load Map");
        btn_loadMap.setFont(new Font("Arial", Font.BOLD, 18));
        btn_loadMap.setMargin(new Insets(10, 15, 10, 15));
        btn_loadMap.setFocusPainted(false);

        btn_loadMap.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {

                // Load map from a map description string
                final JFileChooser fileDialog = new JFileChooser(System
                        .getProperty("user.dir"));
                int returnVal = fileDialog.showOpenDialog(window);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fileDialog.getSelectedFile();

                    try (BufferedReader br = new BufferedReader(new FileReader(
                            file))) {
                        mapUI.loadFromMapString(br.readLine());
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }

                    _loadedMapFilename = file.getName();
//System.out.println("Loaded: " + _loadedMapFilename);
                    JOptionPane.showMessageDialog(window,
                            "Loaded map information from " + file.getName(),
                            "Loaded Map Information",
                            JOptionPane.PLAIN_MESSAGE);
                } else {
                    System.out.println("Open command cancelled by user.");
                }
            }
        });
        mainButtons.add(btn_loadMap);

        JButton btn_saveMap = new JButton("Save Map");
        btn_saveMap.setFont(new Font("Arial", Font.BOLD, 18));
        btn_saveMap.setMargin(new Insets(10, 15, 10, 15));
        btn_saveMap.setFocusPainted(false);

        btn_saveMap.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {

                // Save current map layout to a map descriptor file
                final JFileChooser fileDialog = new JFileChooser(System
                        .getProperty("user.dir"));

                int returnVal = fileDialog.showSaveDialog(window);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    try {
                        String fileName = fileDialog.getSelectedFile() + "";

                        // Allows overriding of existing text files
                        if (!fileName.endsWith(".txt")) {
                            fileName += ".txt";
                        }

                        // Change file writing part to a better implementation
                        FileWriter fw = new FileWriter(fileName);
                        fw.write(mapUI.generateMapString());
                        fw.flush();
                        fw.close();

                        JOptionPane.showMessageDialog(window,
                                "Map information saved to " + fileName,
                                "Saved Map Information",
                                JOptionPane.PLAIN_MESSAGE);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    System.out.println("Save command cancelled by user.");
                }
            }
        });
        mainButtons.add(btn_saveMap);

        JButton btn_Explore = new JButton("Explore");
        btn_Explore.setFont(new Font("Arial", Font.BOLD, 18));
        btn_Explore.setMargin(new Insets(10, 15, 10, 15));
        btn_Explore.setFocusPainted(false);

        btn_Explore.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                // Clear the current map
                System.out.println("Exploring Map..");
                //robotMap.resetRobotMap();

                // Set up the robot
                roboCop.resetRobotState(startPosRow, startPosCol, startDir);
                robotMap.resetRobotMap();
                roboCop.setRobotMap(robotMap);
                roboCop.markStartAsExplored();
                
                // Show the robot map frame
                CardLayout cl = ((CardLayout) mainPanel.getLayout());
                cl.show(mainPanel, "ROBOT MAP");

                // Give the robot map focus
                robotMap.setFocusable(true);
                robotMap.requestFocusInWindow();
                roboCop.setMapUI(mapUI);
                roboCop.startExplore();
            }
        });
        mainButtons.add(btn_Explore);

        JButton btn_FastestPath = new JButton("Fastest Path");
        btn_FastestPath.setFont(new Font("Arial", Font.BOLD, 18));
        btn_FastestPath.setMargin(new Insets(10, 15, 10, 15));
        btn_FastestPath.setFocusPainted(false);

        btn_FastestPath.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                // Clear the current map
                System.out.println("Fastest Path..");
                roboCop.resetRobotState(startPosRow, startPosCol, spStartDir);
                roboCop.startShortestPath(robotMap.generateShortestPathMap());
            }
        });
        mainButtons.add(btn_FastestPath);

        JButton btn_resetMap = new JButton("Reset Map");
        btn_resetMap.setFont(new Font("Arial", Font.BOLD, 18));
        btn_resetMap.setMargin(new Insets(10, 15, 10, 15));
        btn_resetMap.setFocusPainted(false);

        btn_resetMap.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                // Clear the current map
                System.out.println("Resetting map..");
                roboCop.stopExploration();
                roboCop.stopPhysicalExploration();
                roboCop.resetRobotState(RobotConstant.DEFAULT_START_ROW, RobotConstant.DEFAULT_START_COL, RobotConstant.DEFAULT_START_DIR);
                roboCop.stopPhysicalShortestPath();

                robotMap.resetRobotMap();
                // Show the real map (main menu) frame
                CardLayout cl = ((CardLayout) mainPanel.getLayout());
                cl.show(mainPanel, "MAIN");
            }
        });
        mainButtons.add(btn_resetMap);

        JButton btn_testMDF = new JButton("Test MDF");
        btn_testMDF.setFont(new Font("Arial", Font.BOLD, 18));
        btn_testMDF.setMargin(new Insets(10, 15, 10, 15));
        btn_testMDF.setFocusPainted(false);

        btn_testMDF.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (new String(robotMap.generateMDFStringPart1()).equals(mapUI.generateMapString())) {
                    System.out.println("True");
                } else {
                    System.out.println("False");
                }
            }
        });
        mainButtons.add(btn_testMDF);

//        JButton btn_TimeLimited = new JButton("Time Limited Toggle");
//        btn_TimeLimited.setFont(new Font("Arial", Font.BOLD, 18));
//        btn_TimeLimited.setMargin(new Insets(10, 15, 10, 15));
//        btn_TimeLimited.setFocusPainted(false);
//
//        btn_TimeLimited.addMouseListener(new MouseAdapter() {
//            public void mousePressed(MouseEvent e) {
//                roboCop.setTimeLimited();
//            }
//        });
//        mainButtons.add(btn_TimeLimited);
//        
//        JButton btn_CovLimited = new JButton("Coverage Limited Toggle");
//        btn_CovLimited.setFont(new Font("Arial", Font.BOLD, 18));
//        btn_CovLimited.setMargin(new Insets(10, 15, 10, 15));
//        btn_CovLimited.setFocusPainted(false);
//
//        btn_CovLimited.addMouseListener(new MouseAdapter() {
//            public void mousePressed(MouseEvent e) {
//                roboCop.setCoverageLimited();
//            }
//        });
//        mainButtons.add(btn_CovLimited);
        JButton btn_SetMid = new JButton("Set Mid Point");
        btn_SetMid.setFont(new Font("Arial", Font.BOLD, 18));
        btn_SetMid.setMargin(new Insets(10, 15, 10, 15));
        btn_SetMid.setFocusPainted(false);

        btn_SetMid.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                mapUI.toggleMidPoint();
            }
        });
        mainButtons.add(btn_SetMid);

        JButton btn_PhyExplore = new JButton("Physical Explore");
        btn_PhyExplore.setFont(new Font("Arial", Font.BOLD, 18));
        btn_PhyExplore.setMargin(new Insets(10, 15, 10, 15));
        btn_PhyExplore.setFocusPainted(false);

        btn_PhyExplore.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                // Clear the current map
                System.out.println("Exploring Map..");
                //robotMap.resetRobotMap();

                // Set up the robot
                roboCop.resetRobotState(startPosRow, startPosCol, startDir);
                robotMap.resetRobotMap();
                roboCop.setRobotMap(robotMap);
                roboCop.markStartAsExplored();
//                System.out.println("\nRobot Map Row, Col: " + roboCop.getCurrentPosX()
//                        + ", " + roboCop.getCurrentPosY());
//                System.out.println("Robot Direction: " + roboCop.getDirection().toString());

                // Show the robot map frame
                CardLayout cl = ((CardLayout) mainPanel.getLayout());
                cl.show(mainPanel, "ROBOT MAP");

                // Give the robot map focus
                robotMap.setFocusable(true);
                robotMap.requestFocusInWindow();
                roboCop.setMapUI(mapUI);
                roboCop.startPhysicalExplore();
            }
        });
        mainButtons.add(btn_PhyExplore);

        JButton btn_PhyFastest = new JButton("Physical Fastest Path");
        btn_PhyFastest.setFont(new Font("Arial", Font.BOLD, 18));
        btn_PhyFastest.setMargin(new Insets(10, 15, 10, 15));
        btn_PhyFastest.setFocusPainted(false);

        btn_PhyFastest.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                roboCop.startPhysicalShortestPath(robotMap.generateShortestPathMap());
            }
        });
        mainButtons.add(btn_PhyFastest);

        //-----------------------------------------------------------------------------//
        // Show the real map (main menu) buttons by default
        buttonsPanel.add(mainButtons, "BUTTONS");
        CardLayout cl = ((CardLayout) buttonsPanel.getLayout());
        cl.show(buttonsPanel, "BUTTONS");
    }

}
