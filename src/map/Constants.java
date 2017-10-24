/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package map;
import java.awt.Color;
/**
 *
 * @author Chengwei
 */
public final class Constants {
	
	// Grid size - for rendering only
	public static final int GRID_SIZE = 30;
	
	// Map size
	public static final int MAP_COLS = 15;
	public static final int MAP_ROWS = 20;
        
        //Start grid information
        public static final int START_GRID_ROW = 17;
        public static final int START_GRID_COL = 0;
	
	// Goal grid information
	public static final int GOAL_GRID_ROW = 0;
	public static final int GOAL_GRID_COL = 4;
	
	public static final Color C_GRID_LINE = Color.GRAY;
	public static final int GRID_LINE_WEIGHT = 1;
	
	public static final Color C_START = Color.BLUE;
	public static final Color C_GOAL = Color.GREEN;
        public static final Color C_MID = Color.YELLOW;
	
	public static final Color C_UNEXPLORED = Color.LIGHT_GRAY;
	public static final Color C_FREE = Color.WHITE;
	public static final Color C_OBSTACLE = Color.BLACK;
	
	// Prevent instantiation
	private Constants() {}
}
