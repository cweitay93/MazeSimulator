/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package robot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

import robot.RobotConstant.DIRECTION;
import map.Map;
import map.Constants;
import map.MapUI;

public class RobotMap extends Map {

    // For measuring size of the canvas
    private boolean _bMeasured = false;

    // Size of the map
    private int _mapWidth = 0;
    private int _mapHeight = 0;
    // For rendering the map efficiently
    private MapGrid[][] _mapGrids = null;
    private PathGrid[][] _pathGrids = null;

    // Reference to the robot
    private Robot _robot = null;

    // For rendering the robot
    private int _robotOutlineSize = 0;
    private int _robotSize = 0;
    private int[] _arrowX = null;
    private int[] _arrowY = null;

    // For choice of path to render
    private boolean _bShortestPath = false;

    private boolean _bDisplayTruthValues = true;

    public RobotMap(final MapUI mapUI) {
        super();
    }

    public void paintComponent(Graphics g) {
        if (!_bMeasured) {
            _mapWidth = this.getWidth();
            _mapHeight = this.getHeight();

            System.out.println("\nRobotMap Graphics g; Map width: " + _mapWidth
                    + ", Map height: " + _mapHeight);

            // Calculate the map & path grids for rendering
            _mapGrids = new MapGrid[Constants.MAP_ROWS][Constants.MAP_COLS];
            _pathGrids = new PathGrid[Constants.MAP_ROWS][Constants.MAP_COLS];
            for (int mapRow = 0; mapRow < Constants.MAP_ROWS; mapRow++) {
                for (int mapCol = 0; mapCol < Constants.MAP_COLS; mapCol++) {
                    _mapGrids[mapRow][mapCol] = new MapGrid(
                            mapCol * Constants.GRID_SIZE,
                            mapRow * Constants.GRID_SIZE,
                            Constants.GRID_SIZE);

                    _pathGrids[mapRow][mapCol] = new PathGrid(
                            _mapGrids[mapRow][mapCol]);
                }
            }

            _bMeasured = true;
        }

        // Clear the map
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, _mapWidth, _mapHeight);

//        Border border = BorderFactory.createLineBorder(
//				Constants.C_GRID_LINE, MapConstants.GRID_LINE_WEIGHT);
//        this.setBorder(border);
        // Paint the map grids
        Color gridColor;

        for (int mapRow = 0; mapRow < Constants.MAP_ROWS; mapRow++) {
            for (int mapCol = 0; mapCol < Constants.MAP_COLS; mapCol++) {
                g.setColor(Constants.C_GRID_LINE);
                g.fillRect(_mapGrids[mapRow][mapCol].borderX,
                        _mapGrids[mapRow][mapCol].borderY,
                        _mapGrids[mapRow][mapCol].borderSize,
                        _mapGrids[mapRow][mapCol].borderSize);

                // Determine what color to fill grid
                if (_grids[mapRow][mapCol].isExplored()) {
                    if (isStartZone(mapRow, mapCol)) {
                        gridColor = Constants.C_START;
                    } else if (isGoalZone(mapRow, mapCol)) {
                        gridColor = Constants.C_GOAL;
                    } else if (isMidZone(mapRow,mapCol,MapUI.midRow,MapUI.midCol)){
                        gridColor = Constants.C_MID;
                    } else if (_grids[mapRow][mapCol].isObstacle()) {
                        gridColor = Constants.C_OBSTACLE;
                    } else {
                        gridColor = Constants.C_FREE;
                    }
                } else {
                    gridColor = Constants.C_UNEXPLORED;
                }

                g.setColor(gridColor);
                g.fillRect(_mapGrids[mapRow][mapCol].gridX,
                        _mapGrids[mapRow][mapCol].gridY,
                        _mapGrids[mapRow][mapCol].gridSize,
                        _mapGrids[mapRow][mapCol].gridSize);
            }
        } // End outer for loop

        // Draw the traveled path
        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(RobotConstant.PATH_THICKNESS));

        if (_bShortestPath) {
            g2.setColor(RobotConstant.C_SHORTEST_PATH);
        } else {
            g2.setColor(RobotConstant.C_EXPLORE_PATH);
        }

        for (int mapRow = 0; mapRow < Constants.MAP_ROWS; mapRow++) {
            for (int mapCol = 0; mapCol < Constants.MAP_COLS; mapCol++) {
                if (_pathGrids[mapRow][mapCol].cE) {
                    g2.drawLine(_pathGrids[mapRow][mapCol].cX,
                            _pathGrids[mapRow][mapCol].cY,
                            _pathGrids[mapRow][mapCol].eX,
                            _pathGrids[mapRow][mapCol].eY);
                }
                if (_pathGrids[mapRow][mapCol].cN) {
                    g2.drawLine(_pathGrids[mapRow][mapCol].cX,
                            _pathGrids[mapRow][mapCol].cY,
                            _pathGrids[mapRow][mapCol].nX,
                            _pathGrids[mapRow][mapCol].nY);
                }
                if (_pathGrids[mapRow][mapCol].cS) {
                    g2.drawLine(_pathGrids[mapRow][mapCol].cX,
                            _pathGrids[mapRow][mapCol].cY,
                            _pathGrids[mapRow][mapCol].sX,
                            _pathGrids[mapRow][mapCol].sY);
                }
                if (_pathGrids[mapRow][mapCol].cW) {
                    g2.drawLine(_pathGrids[mapRow][mapCol].cX,
                            _pathGrids[mapRow][mapCol].cY,
                            _pathGrids[mapRow][mapCol].wX,
                            _pathGrids[mapRow][mapCol].wY);
                }
            }
        }

        // For showing the truth values of each grid
        if (_bDisplayTruthValues) {

            Font gosuFont = new Font("Arial", Font.PLAIN, 10);
            g2.setFont(gosuFont);

            for (int mapRow = 0; mapRow < Constants.MAP_ROWS; mapRow++) {
                for (int mapCol = 0; mapCol < Constants.MAP_COLS; mapCol++) {

                    g2.setColor(_grids[mapRow][mapCol].isObstacle() ? Color.WHITE
                            : Color.BLACK);
                    double truthValue = _grids[mapRow][mapCol].getTruthValue();
                    g2.drawString(String.format("%3.2f", truthValue),
                            _mapGrids[mapRow][mapCol].gridX + 5,
                            _mapGrids[mapRow][mapCol].gridY + 22);
                }
            }
        }

        // Gets information about the robot
        int robotPosRow = _robot.getCurrentRow();
        int robotPosCol = _robot.getCurrentCol();
        DIRECTION robotDir = _robot.getDirection();

        // Draw the robot outline
        g.setColor(RobotConstant.C_ROBOT_OUTLINE);
        g.fillOval(_mapGrids[robotPosRow][robotPosCol].gridX,
                _mapGrids[robotPosRow][robotPosCol].gridY,
                _robotOutlineSize,
                _robotOutlineSize);

        // Draw the robot
        g.setColor(RobotConstant.C_ROBOT);
        g.fillOval(_mapGrids[robotPosRow][robotPosCol].gridX + 5,
                _mapGrids[robotPosRow][robotPosCol].gridY + 5,
                _robotSize,
                _robotSize);

        // Draw an arrow indicating the robot's direction
        calculateArrowPos(robotPosRow, robotPosCol, robotDir);
        g.setColor(RobotConstant.C_ROBOT_FRONT);
        g.fillPolygon(_arrowX, _arrowY, 3);
    }

    public void resetRobotMap() {

        for (int row = 0; row < Constants.MAP_ROWS; row++) {
            for (int col = 0; col < Constants.MAP_COLS; col++) {
                // Obstacle - Border walls
                if (isBorderWalls(row, col)) {
                    _grids[row][col].resetGrid();
                    //_grids[row][col].markAsObstacle(9.99);
                    _grids[row][col].setExplored(false);
                } else if (isStartZone(row, col) || isGoalZone(row, col)) {
                    _grids[row][col].resetGrid();
                    //_grids[row][col].markAsFreeGrid(9.99);
                    _grids[row][col].setExplored(false);
                } else {
                    _grids[row][col].resetGrid();
                }
            }
        }

        // Clear all path information
        if (_pathGrids != null) {
            resetPathGrids();
        }

        // Reset status
        _bShortestPath = false;
    }
    
    public void setRobot(Robot robot) {
		_robot = robot;
		
		_robotOutlineSize = (Constants.GRID_SIZE -
				(Constants.GRID_LINE_WEIGHT * 2)) * RobotConstant.ROBOT_SIZE;
		_robotSize = _robotOutlineSize - 10;	
    }
    
    private void calculateArrowPos(int robotPosRow, int robotPosCol,
			DIRECTION robotDir) {
		
		int quarterRobotSize = _robotOutlineSize / 4;
		int halfRobotSize = _robotOutlineSize / 2;
		
		int x1 = quarterRobotSize + _mapGrids[robotPosRow][robotPosCol].gridX;
		int y1 = halfRobotSize + _mapGrids[robotPosRow][robotPosCol].gridY;
		
		int x2 = halfRobotSize + _mapGrids[robotPosRow][robotPosCol].gridX;
		int y2 = quarterRobotSize + _mapGrids[robotPosRow][robotPosCol].gridY;
		
		int x3 = x1 + halfRobotSize;
		int y3 = y1;
		
		int x4 = x2;
		int y4 = y2 + halfRobotSize;
		
		switch(robotDir) {
		case EAST:
			_arrowX = new int [] {x2, x3, x4};
			_arrowY = new int [] {y2, y3, y4};
			break;
		case NORTH:
			_arrowX = new int [] {x1, x2, x3};
			_arrowY = new int [] {y1, y2, y3};
			break;
		case SOUTH:
			_arrowX = new int [] {x1, x3, x4};
			_arrowY = new int [] {y1, y3, y4};
			break;
		case WEST:
			_arrowX = new int [] {x1, x2, x4};
			_arrowY = new int [] {y1, y2, y4};
			break;
		}
	}
    
    public void setShortestPath(){
        resetPathGrids();
        _bShortestPath = true;
    }

    public PathGrid[][] getPathGrids() {
        return _pathGrids;
    }

    private void resetPathGrids() {

        if (_pathGrids != null) {
            for (int mapRow = 0; mapRow < Constants.MAP_ROWS; mapRow++) {
                for (int mapCol = 0; mapCol < Constants.MAP_COLS; mapCol++) {
                    _pathGrids[mapRow][mapCol].cE = false;
                    _pathGrids[mapRow][mapCol].cN = false;
                    _pathGrids[mapRow][mapCol].cS = false;
                    _pathGrids[mapRow][mapCol].cW = false;
                }
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

    public class PathGrid {

        public int nX, nY;
        public int eX, eY;
        public int sX, sY;
        public int wX, wY;
        public int cX, cY;

        public boolean cN, cE, cS, cW;

        public PathGrid(MapGrid mapGrid) {

            int halfGridSize = mapGrid.borderSize / 2;
		
            this.nX = mapGrid.borderX + halfGridSize + mapGrid.borderSize;
            this.nY = mapGrid.borderY + mapGrid.borderSize;

            this.eX = mapGrid.borderX + mapGrid.borderSize + mapGrid.borderSize;
            this.eY = mapGrid.borderY + halfGridSize + mapGrid.borderSize;

            this.sX = mapGrid.borderX + halfGridSize + mapGrid.borderSize;
            this.sY = mapGrid.borderY + mapGrid.borderSize + mapGrid.borderSize;

            this.wX = mapGrid.borderX + mapGrid.borderSize;
            this.wY = mapGrid.borderY + halfGridSize + mapGrid.borderSize;

            this.cX = mapGrid.borderX + halfGridSize + mapGrid.borderSize;
            this.cY = mapGrid.borderY + halfGridSize + mapGrid.borderSize;
            
            cN = cE = cS = cW = false;
        }
    }
    
    public String generateMDFStringPart1() {
		
		String mapString = "11"; // First two bits set to 11
                //String mapString = "";
		
		for (int row = (Constants.MAP_ROWS - 1); row >= 0; row--)
                {
                    for (int col = 0; col < Constants.MAP_COLS; col++)
			{
				mapString += _grids[row][col].isExplored() ? "1" : "0";
			}
		}
               
		// Last two bits set to 11
                //return mapString;
		return binaryToHex(mapString + "11");
	}
    
    public String generateMDFStringPart2() {
		
		String mapString = "";
		
		for (int row = (Constants.MAP_ROWS - 1); row >= 0; row--)
                {
                    for (int col = 0; col < Constants.MAP_COLS; col++)
			{
				if(_grids[row][col].isExplored())
					mapString += _grids[row][col].isObstacle() ? "1" : "0";
			}
		}
		
		// Pad with '0' to make the length a multiple of 8
		int mapStringLength = mapString.length();
		int paddingLength = mapStringLength % 8;
		
		if(paddingLength != 0) {
			
			// Find the number of required bits
			paddingLength = 8 - paddingLength;
			
			for(int i = 0; i < paddingLength; i++) {
				mapString += "0";
			}
		}
		
		return binaryToHex(mapString);
	}
    
    public String generateShortestPathMap(){
        String mapString = "11";
        
        for (int row = 0; row < Constants.MAP_ROWS; row++) {
            for (int col = 0; col < Constants.MAP_COLS; col++) {
                if (_grids[row][col].isExplored()) {
                    mapString += _grids[row][col].isObstacle() ? "1" : "0";
                } else {
                    mapString += 1;
                }
            }
        }
        
        mapString += "11";
        return binaryToHex(mapString);
    }
    
    private String binaryToHex(String binaryString) {
		
		int binStrLength = binaryString.length();
		StringBuilder hexSb = new StringBuilder();
		
		for(int charIndex = 0; charIndex < binStrLength; charIndex += 4) {
			
			int value = 0;
			if(binaryString.charAt(charIndex) == '1')
				value += 8;
			if(binaryString.charAt(charIndex + 1) == '1')
				value += 4;
			if(binaryString.charAt(charIndex + 2) == '1')
				value += 2;
			if(binaryString.charAt(charIndex + 3) == '1')
				value += 1;
			
			hexSb.append(Integer.toHexString(value).toUpperCase());
		}
		
		return hexSb.toString();
	}
}
