/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package map;

/**
 *
 * @author Chengwei
 */
public class Grid {
	
	private boolean _bExplored;		// Indicates whether this grid has been explored
	private boolean _bObstacle;		// Indicates whether this grid is an obstacle
	private boolean _bVisited;		// Indicates whether this grid has been visited
	
	private int _row;				// This grid's row within the map
	private int _col;				// This grid's column within the map
	
	private double _truthValue;		// This grid's truth value for the current status
        private boolean _phantomGrid = false;
	
	/**
	 * Default Constructor
	 * 
	 * @author Jin Yao
	 */
	public Grid() {
		_bExplored = false;
		_bObstacle = false;
		_bVisited = false;
		
		_truthValue = 0;
	}
	
	/**
	 * Constructor which initializes the grid's position within the map
	 * 
	 * @param row 	The grid's row within the map
	 * @param col	The grid's column within the map
	 * 
	 * @author Liang Liang
	 */
	public Grid(int row, int col) {
		_row = row;
		_col = col;
		
		_bExplored = false;
		_bObstacle = false;
		_bVisited = false;
		
		_truthValue = 0;
	}
	
	/**
	 * Gets the Grid's row within the Map
	 * 
	 * @return The Grid's row within the Map
	 */
	public int getRow() {
		return _row;
	}
	
	/**
	 * Gets the Grid's column within the Map
	 * 
	 * @return The Grid's column within the Map
	 */
	public int getCol() {
		return _col;
	}
	
	/**
	 * Sets the Grid's row within the Map
	 * 
	 * @param newXCoor The Grid's new x-axis within the Map
	 */
	public void setRow(int newRow) {
		_row = newRow;
	}
	
	/**
	 * Sets the Grid's column within the Map
	 * 
	 * @param newYCoor The Grid's new column within the Map
	 */
	public void setCol(int newCol) {
		_col = newCol;
	}
	
	/**
	 * Indicates whether this Grid has been explored
	 * 
	 * @return True if this Grid has been explored
	 */
	public boolean isExplored() {
		return _bExplored;
	}
	
	/**
	 * Indicates whether this Grid contains an obstacle
	 * 
	 * @return True if this Grid contains an obstacle
	 */
	public boolean isObstacle() {
		return _bObstacle;
	}
	
	/**
	 * Returns the truth value assigned to the current reading for this grid
	 */
	public double getTruthValue() {
		return _truthValue;
	}
	
	/**
	 * Indicates whether this Grid has been visited by the robot
	 * 
	 * @return True if this Grid has been visited by the robot
	 */
	public boolean isVisited() {
		return _bVisited;
	}
	
	/**
	 * Mark this Grid as explored
	 * 
	 * @param bExplored True if this grid has been explored, false otherwise
	 */
	public void setExplored(boolean bExplored) {
		_bExplored = bExplored;
	}
	
	/**
	 * Set this grid as an obstacle
	 * 
	 * @param bObstacle True if this grid is an obstacle, false otherwise
	 */
	public void setObstacle(boolean bObstacle) {
		
		_bExplored = false;
		_bObstacle = bObstacle;
	}
	
	/**
	 * Set this grid as visited
	 * 
	 * @param bVisited True if this grid has been visited, false otherwise
	 */
	public void setVisited(boolean bVisited) {
		
		_bExplored = true;
		_bVisited = bVisited;
	}
	
	/**
	 * Mark this Grid as explored<br>
	 * Mark this Grid as a free grid
	 */
	public void markAsFreeGrid() {
		_bExplored = true;
		_bObstacle = false;
	}
	
	/**
	 * Mark this Grid as explored<br>
	 * Mark this Grid as an obstacle
	 */
	public void markAsObstacle() {
		_bExplored = true;
		_bObstacle = true;
	}
	
	/**
	 * Mark this Grid as explored<br>
	 * Mark this Grid as a free grid<br>
	 * Only if the given truth value is larger than or equal to
	 * the current truth value
	 * 
	 * @param newTruthValue The proposed truth value
	 */
        public void setPhantomGrid(boolean obsCheck){
            _phantomGrid = obsCheck;
        }
        
        public boolean getPhantomGrid() {
            return _phantomGrid;
        }
        
	public void markAsFreeGrid(double newTruthValue) {
		if(newTruthValue >= _truthValue) {
			_bExplored = true;
			_bObstacle = false;
			
			_truthValue = newTruthValue;
                        _phantomGrid = true;
		}
	}
	
	/**
	 * Mark this Grid as explored<br>
	 * Mark this Grid as an obstacle
	 * Only if the given truth value is larger than or equal to
	 * the current truth value
	 * 
	 * @param newTruthValue The proposed truth value
	 */
	public void markAsObstacle(double newTruthValue) {
		if(newTruthValue >= _truthValue) {
			_bExplored = true;
			_bObstacle = true;
			
			_truthValue = newTruthValue;
                        _phantomGrid = true;
		}
	}
	
	/**
	 * Mark this Grid as explored<br>
	 * Mark this Grid as visited
	 */
	public void markAsVisited() {
		_bExplored = true;
		_bVisited = true;
	}
	
	/**
	 * Resets this Grid<p>
	 * This grid will be unexplored, unvisited, and not an obstacle
	 */
	public void resetGrid() {
		_bExplored = false;
		_bVisited = false;
		_bObstacle = false;
		
		_truthValue = 0;
	}
}

