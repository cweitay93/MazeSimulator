/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package map;
import javax.swing.JPanel;
/**
 *
 * @author Chengwei
 */
public class Map extends JPanel {

	protected Grid [][] _grids = null;

	public Map() {
		
		_grids = new Grid [Constants.MAP_ROWS][Constants.MAP_COLS];
		
		for (int row = 0; row < Constants.MAP_ROWS; row++) {
			for (int col = 0; col < Constants.MAP_COLS; col++) {
				_grids[row][col] = new Grid(row, col);
			}
		}
	}

	public void resetMap() {
		
		for (int row = 0; row < Constants.MAP_ROWS; row++) {
			for (int col = 0; col < Constants.MAP_COLS; col++) {
				_grids[row][col].resetGrid();
			}
		}
	}
	
	public boolean isBorderWalls(int row, int col) {
		return (row == 0 || row == (Constants.MAP_ROWS - 1) || col == 0 || col == (Constants.MAP_COLS - 1));
	}
	
	/**
	 * Checks if a given row and column is within the start zone
	 * 
	 * @param row The specified row to check
	 * @param col The specified col to check
	 * @return True if the specified row and column is within the start zone
	 */
	public boolean isStartZone(int row, int col) {
		//return (row >= 0 && row <= 2 && col >= 0 && col <= 2);
                return ((row >= (Constants.START_GRID_ROW)) && (row <= (Constants.START_GRID_ROW +2)) && (col >= (Constants.START_GRID_COL)) && (col <= (Constants.START_GRID_COL +2)));
	}
	
	/**
	 * Checks if a given row and column is within the goal zone
	 * 
	 * @param row The specified row to check
	 * @param col The specified col to check
	 * @return True if the specified row and column is within the goal zone
	 */
	public boolean isGoalZone(int row, int col) {
		//return ((row <= (Constants.MAP_ROWS - 1)) && (row >= (Constants.MAP_ROWS - 3)) && (col <= (Constants.MAP_COLS - 1)) && (col >= (Constants.MAP_COLS - 3)));
                return ((row >= (Constants.GOAL_GRID_ROW)) && (row <= (Constants.GOAL_GRID_ROW +2)) && (col >= (Constants.GOAL_GRID_COL)) && (col <= (Constants.GOAL_GRID_COL +2)));
	}
        
        public boolean isMidZone(int row, int col, int midRow, int midCol) {
            return (row >= midRow && row <= midRow + 2 && col >= midCol && col <= midCol + 2);
        }
	
	public Grid [][] getMapGrids() {
		return _grids;
	}

}

