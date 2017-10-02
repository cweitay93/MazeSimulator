/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package map;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.border.Border;

/**
 *
 * @author Chengwei
 */
public class MapUI extends Map {

    // For measuring size of the canvas
    private boolean _bMeasured = false;
    private boolean _bSetMid = false;

    // Size of the map
    private int _mapWidth = 0;
    private int _mapHeight = 0;
    
    // Mid Point
    
    private int midRow = 0;
    private int midCol = 0;

    // For rendering the map efficiently
    private MapGrid[][] _mapGrids = null;

    public MapUI() {
        super();
//        buildDefaultMap();

        this.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {

                boolean bControlDown = e.isControlDown();

                int mouseClickX = e.getX();
                int mouseClickY = e.getY();

                /*System.out.print(mouseClickX + ", ");
				System.out.print(mouseClickY);
				System.out.println(bControlDown ? ", Control down" : "");*/
                int gridRow = mouseClickY / Constants.GRID_SIZE;
                int gridCol = mouseClickX / Constants.GRID_SIZE;
                System.out.println("(" + gridCol + "," + gridRow + ")");
                if (_bSetMid) {
                    if ((gridRow < Constants.MAP_ROWS && gridRow + 1 < Constants.MAP_ROWS && gridRow + 2 < Constants.MAP_ROWS)
                            && (gridCol < Constants.MAP_COLS && gridCol + 1 < Constants.MAP_COLS && gridCol + 2 < Constants.MAP_COLS)) {
                        if (bControlDown) {
                        } else {
                            boolean midPointAllowed = false;
                            for(int i = 0; i < 3; i++){
                                for(int j = 0; j < 3; j++){
                                    if (_grids[gridRow+i][gridCol+j].isObstacle()){
                                        System.out.println("You cannot set the mid point on an obstacle");
                                        midPointAllowed = false;
                                        return;
                                    } else if(isStartZone(gridRow+i,gridCol+j) || isGoalZone(gridRow+i,gridCol+j)){
                                        System.out.println("You cannot set the mid point on start/goal");
                                        midPointAllowed = false;
                                        return;
                                    } else {
                                        midPointAllowed = true;
                                    }
                                }
                            }
                            if(midPointAllowed)
                                addMidPoint(gridRow, gridCol);
                        }
                    }
                } else {
                    if ((gridRow < Constants.MAP_ROWS)
                            && (gridCol < Constants.MAP_COLS)) {
                        if (bControlDown) {
                            removeObstacle(gridRow, gridCol);
                        } else {
                            addObstacle(gridRow, gridCol);
                        }
                    }
                    System.out.println(generateMapString());
                }
            }
        });
    }

    /*private void buildDefaultMap() {

        for (int row = 0; row < Constants.MAP_ROWS; row++) {
            for (int col = 0; col < Constants.MAP_COLS; col++) {
                // Obstacle - Border walls
                if (isBorderWalls(row, col)) {
                    _grids[row][col].setObstacle(true);
                }
            }
        }
    }*/
    
    public void toggleMidPoint(){
        if(!_bSetMid){
            _bSetMid = true;
            System.out.println("Click on a map grid to set mid point.");
        } else {
            _bSetMid = false;
        }
    }
    
    private void addMidPoint(int row, int col) {
        midRow = row;
        midCol = col;
    }
    
    public boolean isMidZone(int row, int col) {
        return (row >= midRow && row <= midRow + 2 && col >= midCol && col <= midCol + 2);
    }

    private void addObstacle(int row, int col) {
        if (_grids[row][col].isObstacle()) {
            //remove obstacle
            _grids[row][col].setObstacle(false);
        } else if (isStartZone(row, col) || isGoalZone(row, col)) {
            JOptionPane.showMessageDialog(this, "Grid clicked is the start/goal zone. Please try again.", "Warning",
                    JOptionPane.WARNING_MESSAGE);
        } else {
            _grids[row][col].setObstacle(true);
        }
    }

    private void removeObstacle(int row, int col) {
        if (_grids[row][col].isObstacle()) {
            if (isBorderWalls(row, col)) {
                JOptionPane.showMessageDialog(null,
                        "Removing the border walls will cause the robot to"
                        + " fall off the edge of the arena. Please do not"
                        + " attempt to kill the robot!", "Warning",
                        JOptionPane.WARNING_MESSAGE);
            } else {
                _grids[row][col].setObstacle(false);
            }
        }
    }

    public void paintComponent(Graphics g) {

        if (!_bMeasured) {

            _mapWidth = this.getWidth();
            _mapHeight = this.getHeight();

            System.out.println("Map width: " + _mapWidth + ", Map height: " + _mapHeight);

            // Calculate the map grids for rendering
            _mapGrids = new MapGrid[Constants.MAP_ROWS][Constants.MAP_COLS];
            for (int mapRow = 0; mapRow < Constants.MAP_ROWS; mapRow++) {
                for (int mapCol = 0; mapCol < Constants.MAP_COLS; mapCol++) {
                    _mapGrids[mapRow][mapCol] = new MapGrid(mapCol
                            * Constants.GRID_SIZE, mapRow
                            * Constants.GRID_SIZE, Constants.GRID_SIZE);
                }
            }

            _bMeasured = true;
        }

        // Clear the map
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, _mapWidth, _mapHeight);

        Border border = BorderFactory.createLineBorder(
                Constants.C_GRID_LINE, Constants.GRID_LINE_WEIGHT);
        this.setBorder(border);

        // Paint the grids
        for (int mapRow = 0; mapRow < Constants.MAP_ROWS; mapRow++) {
            for (int mapCol = 0; mapCol < Constants.MAP_COLS; mapCol++) {
                g.setColor(Constants.C_GRID_LINE);
                g.fillRect(_mapGrids[mapRow][mapCol].borderX,
                        _mapGrids[mapRow][mapCol].borderY,
                        _mapGrids[mapRow][mapCol].borderSize,
                        _mapGrids[mapRow][mapCol].borderSize);

                Color gridColor = null;

//                if (isBorderWalls(mapRow, mapCol)) {
//                    gridColor = Constants.C_BORDER;
//                } else if (isStartZone(mapRow, mapCol)) {
                // Determine what color to fill grid

                if (isStartZone(mapRow, mapCol)) {
                    gridColor = Constants.C_START;
                } else if (isGoalZone(mapRow, mapCol)) {
                    gridColor = Constants.C_GOAL;
                } else if (isMidZone(mapRow,mapCol)){
                    gridColor = Constants.C_MID;
                } else if (_grids[mapRow][mapCol].isObstacle()) {
                    gridColor = Constants.C_OBSTACLE;
                } else {
                    gridColor = Constants.C_FREE;
                }

                g.setColor(gridColor);
                g.fillRect(_mapGrids[mapRow][mapCol].gridX,
                        _mapGrids[mapRow][mapCol].gridY,
                        _mapGrids[mapRow][mapCol].gridSize,
                        _mapGrids[mapRow][mapCol].gridSize);

            }
        } // End outer for loop	
    } // End paintComponent

    /**
     * Saves the current map to a map descriptor string<br>
     * Not including the virtual border surrounding the area!
     *
     * @return The map descriptor string
     */
    public String generateMapString() {

        String mapString = "";

        for (int row = 0; row < Constants.MAP_ROWS ; row++) 
            {
                for (int col = 0; col < Constants.MAP_COLS; col++) {
                // Obstacle - Border walls
                if (!_grids[row][col].isObstacle()) {
                    mapString += "0";
                } else {
                    mapString += "1";
                }
            }
        }

        return mapString;
    }

    /**
     * Loads the map from a map descriptor string<br>
     * Not including the virtual border surrounding the area!
     */
    public void loadFromMapString(String mapString) {

        for (int row = 0; row < Constants.MAP_ROWS ; row++) 
        {
            for (int col = 0; col < (Constants.MAP_COLS); col++) {
                int charIndex = (row * Constants.MAP_COLS )
                        + col;

                // Obstacle - Border walls
                if (mapString.charAt(charIndex) == '1') {
                    _grids[row][col].setObstacle(true);
                } else {
                    _grids[row][col].setObstacle(false);
                }
            }
        }
    }

    public void clearMap() {

        for (int row = 0; row < (Constants.MAP_ROWS); row++) {
            for (int col = 0; col < (Constants.MAP_COLS); col++) {
                _grids[row][col].setObstacle(false);
            }
        }
    }

    private class MapGrid {

        public int borderX;
        public int borderY;
        public int borderSize;

        public int gridX;
        public int gridY;
        public int gridSize;

        public MapGrid(int borderX, int borderY, int borderSize) {
            this.borderX = borderX;
            this.borderY = borderY;
            this.borderSize = borderSize;

            this.gridX = borderX + Constants.GRID_LINE_WEIGHT;
            this.gridY = borderY + Constants.GRID_LINE_WEIGHT;
            this.gridSize = borderSize - (Constants.GRID_LINE_WEIGHT * 2);
        }
    }
}
