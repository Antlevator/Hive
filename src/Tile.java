import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

public class Tile {
	
	static Map<Point, ArrayList<Tile>> board = new HashMap<>();
	static BufferedImage tilemap;
	static int TW = 332, TH = 302;
	static int stacklimit = 8;
	static double h = 60, rise = 10;
	static Path2D hexagon, hexagon_shadow;
	static Matrix log_to_ren, ren_to_log;
	
	static Point[] nbrpts = new Point[] {
		new Point(-1, 1),
		new Point( 0, 1),
		new Point( 1, 0),
		new Point( 1,-1),
		new Point( 0,-1),
		new Point(-1, 0)
	};
	
	static Map<Integer, Decision> movetrees;
	
	static final int BUG_NONE = 0;
	static final int BUG_ANT = 1;
	static final int BUG_BEE = 2;
	static final int BUG_GRASSHOPPER = 3;
	static final int BUG_LADYBUG = 4;
	static final int BUG_BEETLE = 5;
	static final int BUG_SPIDER = 6;
	static final int BUG_MOSQUITO = 7;
	static final int BUG_MOSQUITO_AS_BEETLE = 8;
	static final int BUG_PILLBUG = 9;
	static final int BUG_DRAGONFLY = 10;
	static final int BUG_DUNG = 11;
	static final int BUG_CLINTON = 12;
	static final int BUG_DUNGBEETLE = 13;
	
	ArrayList<Path2D> possibleMoves = new ArrayList<>();
	int bug = BUG_NONE, mosquito_mask = BUG_NONE;
	int team = 0; // team number doubles as color value
	int lox, loy;
	
	static {
		
		movetrees = new HashMap<>();
		
		//////////////////
		
		// generic tiles have no moves, but this placeholder must exist to prevent null exceptions
		Decision none_seed = new Decision();
		none_seed.addDecision(null);
		movetrees.put(BUG_NONE, none_seed);
		
		//////////////////
		
		// ant decision tree
		Decision ant_seed = new Decision();
		Decision ant_move = new Decision(null, false);
		
		ant_move.retrace = true;
		
		// once moved, the ant can choose to move or stop
		ant_move.addDecision(ant_move);
		ant_move.addDecision(null);
		
		// initially, the ant can only choose to move
		ant_seed.addDecision(ant_move);
		
		// add decision tree to map
		movetrees.put(BUG_ANT, ant_seed);
		
		//////////////////
		
		// bee decision tree
		Decision bee_seed = new Decision();
		Decision bee_move = new Decision(null, false);
		
		// bee single tile move
		bee_seed.addDecision(bee_move);
		bee_move.addDecision(null);
		
		// add decision tree to map
		movetrees.put(BUG_BEE, bee_seed);
		
		//////////////////
		
		// beetle decision tree
		Decision beetle_seed = new Decision();
		Decision beetle_move = new Decision(null, false);
		Decision beetle_climb = new Decision(new Tile(), false);
		
		// beetle's two distinct moves
		beetle_seed.addDecision(beetle_move);
		beetle_seed.addDecision(beetle_climb);
		beetle_climb.addDecision(null);
		beetle_move.addDecision(null);
		
		// add decision tree to map
		movetrees.put(BUG_BEETLE, beetle_seed);
		
		//////////////////
		
		// ladybug decision tree
		Decision ladybug_seed = new Decision();
		Decision ladybug_climb1 = new Decision(new Tile(), false);
		Decision ladybug_climb2 = new Decision(new Tile(), false);
		Decision ladybug_move = new Decision(null, false);
		
//		ladybug_seed.retrace = true;
		ladybug_climb1.retrace = true;
		ladybug_climb2.retrace = true;
		ladybug_move.retrace = true;
		
		// ladybug's specific ordering and number of moves
		ladybug_seed.addDecision(ladybug_climb1);
		ladybug_climb1.addDecision(ladybug_climb2);
		ladybug_climb2.addDecision(ladybug_move);
		ladybug_move.addDecision(null);
		
		// add decision tree to map
		movetrees.put(BUG_LADYBUG, ladybug_seed);
		
		//////////////////
		
		// grasshopper decision tree
		Decision gh_seed = new Decision();
		Decision gh_climb = new Decision(new Tile(), true);
		Decision gh_move = new Decision(null, true);
		
		gh_seed.retrace = true;
		
		gh_seed.addDecision(gh_climb);
		gh_climb.addDecision(gh_climb);
		gh_climb.addDecision(gh_move);
		gh_move.addDecision(null);
		
		movetrees.put(BUG_GRASSHOPPER, gh_seed);
		
		//////////////////
		
		Decision spider_seed = new Decision();
		Decision spider_move1 = new Decision(null, false);
		Decision spider_move2 = new Decision(null, false);
		Decision spider_move3 = new Decision(null, false);
		
		spider_seed.retrace = true;
		spider_move1.retrace = true;
		spider_move2.retrace = true;
		spider_move3.retrace = true;
		
		spider_seed.addDecision(spider_move1);
		spider_move1.addDecision(spider_move2);
		spider_move2.addDecision(spider_move3);
		spider_move3.addDecision(null);
		
		movetrees.put(BUG_SPIDER, spider_seed);
		
		//////////////////
		
		Decision pillbug_seed = new Decision();
		Decision pillbug_move = new Decision(null, false);
		
		pillbug_seed.addDecision(pillbug_move);
		pillbug_move.addDecision(null);
		
		movetrees.put(BUG_PILLBUG, pillbug_seed);
		
		//////////////////
		
		Decision df_seed = new Decision();
		Decision df_dir = new Decision(new Tile(), true);
		Decision df_climb = new Decision(new Tile(), false);
		
		df_seed.retrace = true;
		df_dir.retrace = true;
		df_climb.retrace = true;
		
		df_seed.addDecision(df_dir);
		df_dir.addDecision(df_climb);
		df_dir.addDecision(null);
		df_climb.addDecision(df_dir);
		df_climb.addDecision(null);
		
		movetrees.put(BUG_DRAGONFLY, df_seed);
		
		//////////////////
		
		Decision dung_seed = new Decision();
		dung_seed.addDecision(null);
		movetrees.put(BUG_DUNG, dung_seed);
		
		//////////////////
		
		// clinton decision tree
		Decision clinton_seed = new Decision();
		Decision clinton_move = new Decision(null, false);
		Decision clinton_climb = new Decision(new Tile(), false);

		// clinton's two distinct moves
		clinton_seed.addDecision(beetle_move);
		clinton_seed.addDecision(beetle_climb);
		clinton_climb.addDecision(null);
		clinton_move.addDecision(null);
		
		movetrees.put(BUG_CLINTON, clinton_seed);
		
		//////////////////
		
		Decision db_seed = new Decision();
		Decision db_move = new Decision(null, false);
		
		db_seed.addDecision(db_move);
		db_move.addDecision(null);
		
		movetrees.put(BUG_DUNGBEETLE, db_seed);
		
		//////////////////
		
		try {
			URL curl = Tile.class.getResource("bug_generic2.png");
			if(curl == null) {
				JOptionPane.showConfirmDialog(null, "Couldnt be found");
			}
			tilemap = toBufferedImage(ImageIO.read(curl));
		} catch (IOException e) {
			e.printStackTrace();
			File f = new File("failure.log");
			try {
				f.createNewFile();
				
				BufferedWriter bw = new BufferedWriter(new FileWriter(f));
				File ff = new File(".");
				for(String cf : ff.list()) {
					bw.write(cf + "\r\n");
				}
				bw.close();
				
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		log_to_ren = new Matrix(2, 2);
		Matrix.log_to_ren_matrix(log_to_ren, h);
		ren_to_log = new Matrix(2, 2);
		Matrix.ren_to_log_matrix(ren_to_log, h);
		
		double h2 = h * 2 / Math.sqrt(3);
		double shadow_shift = 0;
		hexagon = new Path2D.Double();
		hexagon_shadow = new Path2D.Double();
		hexagon.moveTo(h2/2, 0);
		hexagon_shadow.moveTo(h/2, 0);
		for(int i = 0; i < 6; i++) {
			double angle = i * Math.PI * 2 / 6;
			hexagon.lineTo(h2/2 * Math.cos(angle), h2/2 * Math.sin(angle));
			if(i == 0) {
				hexagon_shadow.lineTo(h2/2 * Math.cos(angle), h2/2 * Math.sin(angle) + shadow_shift);
				shadow_shift = 5;
			}
			hexagon_shadow.lineTo(h2/2 * Math.cos(angle), h2/2 * Math.sin(angle) + shadow_shift);
			if(i == 3) {
				shadow_shift = 0;
				hexagon_shadow.lineTo(h2/2 * Math.cos(angle), h2/2 * Math.sin(angle) + shadow_shift);
			}
		}
		hexagon.closePath();
		hexagon_shadow.closePath();
	}
	
	public Tile(int lox, int loy, int team) {
		this.lox = lox;
		this.loy = loy;
		this.team = team;
		this.bug = (int)(Math.random() * 8);
		setPositionInMap();
	}
	
	public Tile(int lox, int loy, int team, int bug) {
		this(lox, loy, team);
		this.bug = bug;
	}
	
	public Tile(int lox, int loy, int team, int bug, boolean real) {
		this(lox, loy, team);
		this.bug = bug;
		if(!real) {
			removeFromMap();
		}
	}
	
	public Tile copy() {
		Tile copy = new Tile();
		copy.lox = lox;
		copy.loy = loy;
		copy.team = team;
		copy.bug = bug;
		copy.mosquito_mask = mosquito_mask;
		return copy;
	}
	
	/* dont use me */
	public Tile() {}

	public ArrayList<Path2D> findPossibleMoves(int teamcolor) {
		possibleMoves = findPossibleMoves(this, teamcolor);
		return possibleMoves;
	}
	
	public void clean() {
		possibleMoves.clear();
	}
	
	public static ArrayList<Path2D> findPossibleMoves(Tile cur, int teamcolor) {
		
		boolean team_conflict = cur.team != teamcolor;
		
		ArrayList<Path2D> moves = new ArrayList<>();
		ArrayList<Point> currentpath = new ArrayList<>();
		ArrayList<Point> covered = new ArrayList<>();
		
		cur.removeFromMap();
		
		if(!team_conflict) {
			if(cur.bug != BUG_MOSQUITO) {
				RealDecision init = new RealDecision(movetrees.get(cur.bug), cur.toPoint());
				decisionsRecursive(moves, currentpath, covered, init, null);
			} else {
				// if the mosquito is in a position that forces it to continue acting as a specific bug (beetle)
				if(cur.mosquito_mask != BUG_NONE) {
					RealDecision init = new RealDecision(movetrees.get(cur.mosquito_mask), cur.toPoint());
					decisionsRecursive(moves, currentpath, covered, init, null);	
				// if the mosquito is unmasked, and free to take on its neighbors forms
				} else {
					Point[] nbrs = getNeighborTiles(cur.toPoint());
					for(int i = 0; i < nbrs.length; i++) {
						if(nbrs[i] != null) {
							ArrayList<Tile> nbrstack = Tile.board.get(nbrs[i]);
							// is there a valid tile to steal moves from at this neighbor's position?
							if(nbrstack != null && !nbrstack.isEmpty()) {
								Tile top = nbrstack.get(nbrstack.size() - 1);
								// mosquito cant steal mosquito's moves
								if(top.bug != BUG_MOSQUITO) {
									covered.clear(); // in case different bugs use different covered rules (such as with/without retrace)
									RealDecision init = new RealDecision(movetrees.get(nbrstack.get(nbrstack.size() - 1).bug), cur.toPoint());
									decisionsRecursive(moves, currentpath, covered, init, null);
								}
							}
						}
					}
				}
			}
		}
		
		if(Tile.board.get(cur.toPoint()) == null) {
		
			ArrayList<Tile> stack = Tile.board.get(cur.toPoint());
			if(stack != null && stack.get(stack.size() - 1).bug == BUG_CLINTON) {
				Path2D path = new Path2D.Double();
				path.moveTo(cur.lox, cur.loy);
				optimizePathWithEndpoint(moves, path);
			}
			
			Point[] nbrs = getNeighborTiles(cur.toPoint());
			
			for(int i = 0; i < nbrs.length; i++) {
				// determine if pillbug hop is available with this neighbor
				if(nbrs[i] != null) {
					ArrayList<Tile> nbrstack = Tile.board.get(nbrs[i]);
					if(nbrstack != null && nbrstack.size() > 0) {
						Tile top = nbrstack.get(nbrstack.size() - 1);
						boolean pillbug_present = top.bug == BUG_PILLBUG && top.team == teamcolor;
						
						if(!pillbug_present && top.bug == BUG_MOSQUITO && top.team == teamcolor) {
							pillbug_present = hasNeighborOfType(nbrs[i], BUG_PILLBUG);
						}
						
						if(pillbug_present) {
							Point hoppt = new Point(cur.toPoint().x + (nbrs[i].x - cur.toPoint().x) * 2, cur.toPoint().y + (nbrs[i].y - cur.toPoint().y) * 2);
							ArrayList<Tile> hoppos = Tile.board.get(hoppt);
							if((hoppos == null || hoppos.size() == 0) /*&& !anyPathHasEndpoint(moves, hoppt)*/) {
								
								Path2D path = new Path2D.Double();
								path.moveTo(cur.toPoint().x, cur.toPoint().y);
								path.lineTo(nbrs[i].x, nbrs[i].y);
								path.lineTo(hoppt.x, hoppt.y);
								
								// add path if it is more optimal than the current path to this point, or if no path currently exists
								// which goes to this point
								optimizePathWithEndpoint(moves, path);
	//							moves.add(path);
							}
						}
					}
				}
			}
		
		}
		
		cur.setPositionInMap();
		
		return moves;
	}
	
	public static boolean hasNeighborOfType(Point p, int target_type) {
		Point[] nbrs = getNeighborTiles(p);
		for(int i = 0; i < nbrs.length; i++) {
			// determine if bug of type is neighboring
			if(nbrs[i] != null) {
				ArrayList<Tile> nbrstack = Tile.board.get(nbrs[i]);
				if(nbrstack != null && nbrstack.size() > 0) {
					Tile top = nbrstack.get(nbrstack.size() - 1);
					if(top.bug == target_type) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public static boolean hasNeighborOfTypeAndTeam(Point p, int target_type, int target_team) {
		Point[] nbrs = getNeighborTiles(p);
		for(int i = 0; i < nbrs.length; i++) {
			// determine if bug of type is neighboring
			if(nbrs[i] != null) {
				ArrayList<Tile> nbrstack = Tile.board.get(nbrs[i]);
				if(nbrstack != null && nbrstack.size() > 0) {
					Tile top = nbrstack.get(nbrstack.size() - 1);
					if(top.bug == target_type && top.team == target_team) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public static void decisionsRecursive(ArrayList<Path2D> record, ArrayList<Point> currentpath, ArrayList<Point> covered, RealDecision cur, Point dir) {
		
		// add self to the list of points that have been covered
		covered.add(cur.pos);
		currentpath.add(cur.pos);
		
		// add the current position to the list of moves if null (stop) is an available next decision
		for(int i = 0; i < cur.next.size(); i++) {
			if(cur.next.get(i) == null /*&& !anyPathHasEndpoint(record, cur.pos)*/) {
				// make a copy of the path to the current position from the start position using valid decisions for animations / debugging
				Path2D pathcopy = new Path2D.Double();
				pathcopy.moveTo(currentpath.get(0).x, currentpath.get(0).y);
				for(int j = 1; j < currentpath.size(); j++) {
					pathcopy.lineTo(currentpath.get(j).x, currentpath.get(j).y);
				}
//				record.add(pathcopy);
				optimizePathWithEndpoint(record, pathcopy);
			}
		}
		
		// the piece is free to move in any direction, so iterate over all directions
		for(int i = 0; i < nbrpts.length; i++) {
			if(!cur.locked_direction || nbrpts[i].equals(dir)) {
				Point nextpt = new Point(cur.pos.x + nbrpts[i].x, cur.pos.y + nbrpts[i].y);
				ArrayList<Tile> nextpos = Tile.board.get(nextpt);
				
				for(Decision next : cur.next) {
					// skip over turn ending decision
					if(next == null) { continue; }
					// if type of decision and tile in that direction are equal, recurse on that tile
					if((next.type == null) == (nextpos == null)) {
						
						// if we haven't already traversed this position
						if(!covered.contains(nextpt)) {
							
							// make sure we wont jump off the hive
							if((cur.type == null && Tile.getNeighborTileCount(nextpt) > 0) || cur.type != null) {
								
								ArrayList<Tile> atpos = Tile.board.get(cur.pos);
								boolean nextIsEmpty = nextpos == null || nextpos.size() <= 0;
								boolean currIsEmpty = atpos == null || atpos.size() <= 0;
								
								// check either that this move is above the board, or that we aren't squeezing to get there
								if(!nextIsEmpty || !currIsEmpty || !checkIfSqueezing(cur.pos, nextpt)) {
									
									// determine if move jumps over gap and separates piece from hive
									Point[] nbrs = getSharedNeighbors(cur.pos, nextpt);
									if(!(nextpos == null && atpos == null && getNeighborTileCount(nbrs) == 0)) {
										RealDecision nextdec = new RealDecision(next, nextpt);
										decisionsRecursive(record, currentpath, covered, nextdec, nbrpts[i]);
									}
								}
							}
						}
					}
				}
			}
		}
		
		// if we are allowed to retrace our steps, 
		if(cur.retrace) {
			covered.remove(cur.pos);
		}
		
		// backtrack to earlier path, allowing for new branching paths given alternate decisions at this point
		currentpath.remove(currentpath.size() - 1);
	}
	
	public static boolean anyPathHasEndpoint(ArrayList<Path2D> record, Point pos) {
		for(Path2D path : record) {
			Point2D pt2D = path.getCurrentPoint();
			Point pt = new Point((int)pt2D.getX(), (int)pt2D.getY());
			if(pt.equals(pos)) {
				return true;
			}
		}
		return false;
	}
	
	public static Path2D getPathWithEndpoint(ArrayList<Path2D> paths, Point end) {
		for(Path2D path : paths) {
			if(path.getCurrentPoint().equals(end)) {
				return path;
			}
		}
		return null;
	}
	
	public static void optimizePathWithEndpoint(ArrayList<Path2D> paths, Path2D newpath) {
		Point2D endpoint = newpath.getCurrentPoint();
		Path2D oldpath = null;
		for(Path2D path : paths) {
			if(path.getCurrentPoint().equals(endpoint)) {
				oldpath = path;
			}
		}
		if(oldpath == null) {
			paths.add(newpath);
			return;
		}
		int oldlen = getLengthOfPath(oldpath);
		int newlen = getLengthOfPath(newpath);
		if(newlen < oldlen) {
			paths.remove(oldpath);
			paths.add(newpath);
		}
	}
	
	public static int getLengthOfPath(Path2D path) {
		int ct = 0;
		PathIterator pi = path.getPathIterator(null);
		while(!pi.isDone()) {
			ct++;
			pi.next();
		}
		return ct;
	}
	
	public static ArrayList<Point> convertPathToPoints(Path2D path) {
		ArrayList<Point> pts = new ArrayList<>();
		PathIterator pi = path.getPathIterator(null);
		double[] coords = new double[6];
		while(!pi.isDone()) {
			int type = pi.currentSegment(coords);
			if(type != PathIterator.SEG_CLOSE) {
				pts.add(new Point((int)coords[0], (int)coords[1]));
			}
			pi.next();
		}
		return pts;
	}

	private static ArrayList<Point> path(Point p1, Point p2) {
		ArrayList<Point> covered = new ArrayList<Point>();
		ArrayList<Point> path = new ArrayList<Point>();
		if(recursePath(covered, path, p1, p2)) {
			return path;
		} else {
			return null;
		}
	}
	
	private static boolean recursePath(ArrayList<Point> covered, ArrayList<Point> path, Point current, Point dest) {
		covered.add(current);
		Point[] nbrs = getNeighborTiles(current);
		for(int i = 0; i < nbrs.length; i++) {
			if(dest.equals(nbrs[i])) {
				path.add(nbrs[i]);
				return true;
			}
			if(nbrs[i] != null && !covered.contains(nbrs[i])) {
				boolean found = recursePath(covered, path, nbrs[i], dest);
				if(found) {
					path.add(0, nbrs[i]);
					return true;
				}
			}
		}
		return false;
	}

	private static Point getRenderCoordinate(Point p) {
		Vector v = new Vector(p.getX(), p.getY());
		Vector res = Vector.multiply(log_to_ren, v);
		return res.toPoint();
	}
	
	public static Point2D getRenderCoordinate(Point2D p) {
		Vector v = new Vector(p.getX(), p.getY());
		Vector res = Vector.multiply(log_to_ren, v);
		return res.toPoint2D();
	}

	public static BufferedImage toBufferedImage(Image image) {
		BufferedImage bimg = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics g = bimg.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return bimg;
	}
	
	public void setPositionInMap() {
		Point pos = new Point(lox, loy);
		ArrayList<Tile> atpos = board.get(pos);
		// If this position has never existed before, make it
		if(atpos == null) {
			System.out.println("Created new position at ("+lox+", "+loy+")");
			atpos = new ArrayList<Tile>();
			atpos.add(this);
			board.put(pos, atpos);
		} else {
			atpos.add(this);
			System.out.println("Added "+this+" to new position ("+lox+", "+loy+")");	
		}
	}
	
	/**
	 * Removes piece from where it is located in the map
	 */
	public void removeFromMap() {
		Point thispt = new Point(this.lox, this.loy);
		// remove self from current position
		ArrayList<Tile> atpos = board.get(thispt);
		boolean wasInList = atpos.remove(this);
		if(!wasInList) {
			throw new IllegalStateException("Attempted to remove tile from a position where it was not located!");
		}
		// remove lists of size 0, blank spaces need not be stored
		if(atpos.isEmpty()) {
			board.remove(thispt);
		}
		System.out.println("Removed "+this+" from position ("+lox+", "+loy+")");
	}
	
	public void changePositionInMap(int lox, int loy) {
		removeFromMap();
		this.lox = lox;
		this.loy = loy;
		setPositionInMap();
	}
	
	public void draw(Graphics go, Point p) {
		
		Vector vm = new Vector(p.getX(), p.getY());
		Vector resm = Vector.multiply(ren_to_log, vm);
		
		Graphics2D g = (Graphics2D) go.create();
		Vector v = new Vector(lox, loy);
		
		Vector res = Vector.multiply(log_to_ren, v);
		g.translate(res.x(), res.y() - getRenderRise());
		
//		Point2D offs = getSpriteSize();
//		g.translate(-offs.getX()/2, -offs.getY()/2);
//		g.drawImage(getTileSubimage(), 0, 0, (int)offs.getX(), (int)offs.getY(), null);
		drawTileSprite(g, 1);
//		g.translate(offs.getX()/2, offs.getY()/2);
		
		if(Math.abs(v.x() - resm.x()) <= .5 && Math.abs(v.y() - resm.y()) <= .5) {
			g.setColor(new Color(155, 255, 100, 80));
			g.fill(hexagon);
		}
		
//		g.setColor(Color.white);
//		g.drawString(lox + ", " + loy, 0, 0);
		g.dispose();
	}
	
	public static Point2D getSpriteSize() {
		return getSpriteSize(1);
	}
	
	public static Point2D getSpriteSize(double spriteScale) {
		double expand = 1.07;
		double hxp = h * expand;
		double wxp = hxp * TW / TH;
		return new Point2D.Double(wxp * spriteScale, hxp * spriteScale);
	}
	
	public void drawReachables(Graphics go) {
		Graphics2D g = (Graphics2D) go.create();
		
		if(possibleMoves != null) {
			for(int i = 0; i < possibleMoves.size(); i++) {
				g.setColor(Color.black);
				renderPathFromHexCoordinates(g, possibleMoves.get(i));
				g.setColor(new Color(255, 255, 100, 80));
				Point2D pt2D = possibleMoves.get(i).getCurrentPoint();
				Point pt = new Point((int)pt2D.getX(), (int)pt2D.getY());
				drawHexAtLogicalCoordinate(g, pt, true);
			}
		}
		
		g.dispose();
	}
	
	private void renderPathFromHexCoordinates(Graphics2D g, Path2D path) {
		Path2D renpath = new Path2D.Double();
		PathIterator pi = path.getPathIterator(null);
		double[] pathpt = new double[6];
		while(!pi.isDone()) {
			int segtype = pi.currentSegment(pathpt);
			if(segtype != PathIterator.SEG_CLOSE) {
				Point rencoord = Tile.getRenderCoordinate(new Point((int)pathpt[0], (int)pathpt[1]));
				if(renpath.getCurrentPoint() == null) {
					renpath.moveTo(rencoord.x, rencoord.y);
				} else {
					renpath.lineTo(rencoord.x, rencoord.y);
				}
			}
			pi.next();
		}
		g.draw(renpath);
	}
	
//	private static int getNeighborTileCount(Point p2) {
//		Point[] nbrs = getNeighbors(p2);
//		int ct = 0;
//		for(int i = 0; i < 6; i ++) {
//			if(Tile.board.get(nbrs[i]) != null) {
//				ct++;
//			}
//		}
//		return ct;
//	}

	public double getRenderX() {
		return lox * h * Math.cos(Math.PI / 6) +
			   loy * h * Math.cos(Math.PI / 6);
	}
	
	public double getRenderY() {
		return lox * h * Math.sin(Math.PI / 6) -
			   loy * h * Math.sin(Math.PI / 6);
	}
	
	public double getRenderRise() {
		ArrayList<Tile> atpos = board.get(new Point(lox, loy));
		int idx = atpos.indexOf(this);
		return idx * rise;
	}
	
	public static void drawHexAt(Graphics go, Point p, boolean fill) {
		Graphics2D g = (Graphics2D) go.create();
		Vector res = getLogicalHexGridCoordinate(p);
		Vector res2 = Vector.multiply(log_to_ren, res);
		g.translate(res2.x(), res2.y());
		if(fill) {
			g.fill(hexagon);
		} else {
			g.draw(hexagon);
		}
		g.dispose();
	}
	
	public static void drawHexAtLogicalCoordinate(Graphics go, Point p, boolean fill) {
		drawHexAtLogicalCoordinate(go, p, fill, 1);
	}
	
	public static void drawHexAtLogicalCoordinate(Graphics go, Point p, boolean fill, double scale) {
		Graphics2D g = (Graphics2D) go.create();
		Vector pvec = new Vector(p.x, p.y);
		Vector res2 = Vector.multiply(log_to_ren, pvec);
		g.translate(res2.x(), res2.y());
		g.scale(scale, scale);
		if(fill) {
			Color c = g.getColor();
			g.setColor(c.darker());
			g.fill(hexagon_shadow);
			g.setColor(c);
			g.fill(hexagon);
		} else {
			g.draw(hexagon);
		}
		g.dispose();
	}
	
	public static Vector getLogicalHexGridCoordinate(Point p) {
		Vector v = new Vector(p.getX(), p.getY());
		Vector res = Vector.multiply(ren_to_log, v);
		res.divideFloorMultiply(1);
		return res;
	}

	public boolean select(Point p) {
		
		boolean selected = false;
		
		Vector vm = new Vector(p.getX(), p.getY());
		Vector resm = Vector.multiply(ren_to_log, vm);
		Vector v = new Vector(lox, loy);
		
		if(Math.abs(v.x() - resm.x()) <= .5 && Math.abs(v.y() - resm.y()) <= .5) {
			selected = true;
		}
		
		return selected;
	}

	public boolean attemptMove(Point2D pt) {
		
		boolean success = true, stacked = false;
		
		// we only want to check board validity when this piece could change its shape
		ArrayList<Tile> atpos = Tile.board.get(new Point(lox, loy));
		if(atpos != null && atpos.size() > 1) {
			System.out.println("Jumped from stack");
			stacked = true;
		}
		
		int olox = lox;
		int oloy = loy;
		changePositionInMap((int)pt.getX(), (int)pt.getY());
		
		if(!stacked) {
			System.out.println("Checking validity of board...");
			if(!validBoard()) {
				System.out.println("Board was not valid! Moving back...");
				changePositionInMap(olox, oloy);
				success = false;
			}
		}
		
		// determine if the mosquito needs to be masked or unmasked
		if(success = true && bug == BUG_MOSQUITO) {
			atpos = Tile.board.get(new Point(lox, loy));
			if(atpos != null && atpos.size() > 1) {
				mosquito_mask = BUG_BEETLE;
			} else {
				mosquito_mask = BUG_NONE;
			}
		}
		
		return success;
	}

	static ArrayList<Point> traversed = new ArrayList<>();
	static int travcount = 0;
	public static boolean validBoard() {
		
		// reset traversal global variables
		traversed.clear();
		travcount = 0;
		
		if(Tile.board.isEmpty()) {
			return false;
		}
		
		// get a single "seed" entry in the set of all positions
		Entry<Point, ArrayList<Tile>> seed = Tile.board.entrySet().iterator().next();
		traversed.add(seed.getKey());
		traverseChildren(seed.getKey());
		
		System.out.println("travcount: " + travcount + ", real count: " + Tile.board.size());
		
		if(travcount != Tile.board.size()) {
			System.out.println("Board has different number of tiles, means some werent reachable!");
			return false;
		}
		
		return true;
	}
	
	public static boolean canMoveWithoutBreakingHive(Point p) {
		System.out.println("Checking if piece can move without breaking the hive");
		boolean success = true;
		ArrayList<Tile> atpos = Tile.board.get(p);
		if(atpos == null) {
			System.out.println("No pieces at this location!");
			return false;
		}
		assert(atpos.size() > 0);
		if(atpos.size() > 1) {
			return true;
		}
		Tile t = atpos.get(0);
		t.removeFromMap();
		if(!validBoard()) {
			success = false;
		}
		t.setPositionInMap();
		return success;
	}

	public static void traverseChildren(Point pt) {
		
		// make sure we arent traversing anything more than once
		travcount++;
		// traverse all children in the hex grid
		for(int i = -1; i <= 1; i++) {
			for(int j = -1; j <= 1; j++) {
				// this necessary condition prevents too distant neighbors and self traversal
				if(i != j) {
					Point nbrpt = new Point(pt.x + i, pt.y + j);
					// if we have not traversed this position before
					if(!traversed.contains(nbrpt)) {
						traversed.add(nbrpt);
						// if there is actually something at this position
						if(Tile.board.get(nbrpt) != null) {
							// traverse all children of this position, enter stack frame hell
							traverseChildren(nbrpt);
						}
					}
				}
			}
		}
	}
	
	public static boolean checkIfSqueezing(Point cur, Point point) {
		// determine good algorithm for this, i think it requires pathfinding
		Point diff = new Point(point.x - cur.x, point.y - cur.y);
		
		Point[] shared = getSharedNeighbors(cur, point);
		
		int idx = 0;
		for(int i = 0; i < 2; i++) {
			if(Tile.board.get(shared[i]) != null) {
				idx++;
			}
		}
		
		if(idx > 1) {
			return true;
		}
		
		return false;
	}
	
	public static Point[] getNeighbors(Point p) {
		Point[] nbrs = new Point[6];
		int idx = 0;
		for(int i = -1; i <= 1; i++) {
			for(int j = -1; j <= 1; j++) {
				if(i != j) {
					nbrs[idx++] = new Point(p.x + i, p.y + j);
				}
			}
		}
		return nbrs;
	}
	
	public static Point[] getNeighborTiles(Point p) {
		Point[] nbrs = new Point[6];
		int idx = 0;
		for(int i = -1; i <= 1; i++) {
			for(int j = -1; j <= 1; j++) {
				if(i != j) {
					Point nbr = new Point(p.x + i, p.y + j);
					if(Tile.board.get(nbr) != null) {
						nbrs[idx++] = nbr;
					}
				}
			}
		}
		return nbrs;
	}
	
	public static int getNeighborTileCount(Point p) {
		int idx = 0;
		for(int i = -1; i <= 1; i++) {
			for(int j = -1; j <= 1; j++) {
				if(i != j) {
					if(Tile.board.get(new Point(p.x + i, p.y + j)) != null) {
						idx++;
					}
				}
			}
		}
		return idx;
	}
	
	public static int getNeighborTileCount(Point[] nbrs) {
		int idx = 0;
		for(int i = 0; i < nbrs.length; i++) {
			if(Tile.board.get(nbrs[i]) != null) {
				idx++;
			}
		}
		return idx;
	}
	
//	public static Point[] getSharedNeighbors(Point p1, Point p2) {
//		Point[] nbrs3 = new Point[6];
//		if(Math.abs(p1.x - p2.x) > 1 || Math.abs(p1.y - p2.y) > 1) {
//			return nbrs3;
//		}
//		Point[] nbrs1 = getNeighbors(p1);
//		Point[] nbrs2 = getNeighbors(p2);
//		int idx = 0;
//		for(int i = 0; i < 6; i++) {
//			for(int j = 0; j < 6; j++) {
//				if(nbrs1[i].equals(nbrs2[j])) {
//					nbrs3[idx++] = nbrs1[i];
//				}
//			}
//		}
//		return nbrs3;
//	}
	
	public static Point[] getSharedNeighbors(Point p1, Point p2) {
		Point[] snbrs = new Point[2];
		
		// strange cases
		if(p1 == null || p2 == null) {
			return snbrs; // nulls, no shared neighbors
		}
		if(p1.equals(p2)) {
			return getNeighbors(p1); // all neighbors are shared with self
		}
		// too far to share neighbors
		if(Math.abs(p1.x - p2.x) > 1 || Math.abs(p1.y - p2.y) > 1) {
			return snbrs; // nulls, no shared neighbors
		}
		
		// construct relative neighbor position of p2 to p1
		Point nbrpt = new Point(p2.x - p1.x, p2.y - p1.y);
		
		// find the index of that relative neighbor position in the ring
		int nbridx = -1;
		for(int i = 0; i < nbrpts.length; i++) {
			if(nbrpts[i].equals(nbrpt)) {
				nbridx = i;
				break;
			}
		}
		
		// if it didnt exist, something went wrong, these points arent neighbors
		if(nbridx == -1) {
			throw new IllegalStateException("Neighbor does not exist! (" + nbrpt.x + ", " + nbrpt.y + ")");
		}
		
		// find the adjacent ring elements
		int snbridx1 = (nbridx - 1 < 0)? 5 : (nbridx - 1);
		int snbridx2 = (nbridx + 1 > 5)? 0 : (nbridx + 1);
		
		// set shared neighbors to absolute versions of these relative coordinates
		snbrs[0] = new Point(p1.x + nbrpts[snbridx1].x, p1.y + nbrpts[snbridx1].y);
		snbrs[1] = new Point(p1.x + nbrpts[snbridx2].x, p1.y + nbrpts[snbridx2].y);
		
		return snbrs;
	}

	public Point toPoint() {
		return new Point(lox, loy);
	}

	public BufferedImage getTileSubimage() {
		int usebug = bug;
		if(mosquito_mask == BUG_BEETLE) {
			usebug = BUG_MOSQUITO_AS_BEETLE;
		}
		return tilemap.getSubimage(usebug * TW, 0, TW, TH);
	}
	
	public void drawTileSprite(Graphics2D g, double scale) {
		Point2D size = Tile.getSpriteSize(scale);
		g.setColor(new Color(team));
		drawHexAtLogicalCoordinate(g, new Point(0, 0), true, scale);
		g.translate(-size.getX() / 2, -size.getY() / 2);
		g.drawImage(getTileSubimage(), 0, 0, (int)size.getX(), (int)size.getY(), null);
		g.translate(size.getX() / 2, size.getY() / 2);
	}

	public void getConfetti(ArrayList<Confetti> confetti) {
		Point2D size = Tile.getSpriteSize(1);
		BufferedImage bimg = new BufferedImage((int)size.getX(), (int)size.getY(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) bimg.getGraphics();
		g.translate(size.getX() / 2, size.getY() / 2);
		drawTileSprite(g, 1);
		g.dispose();
		Point2D ren = Tile.getRenderCoordinate(toPoint());
		int rx = (int)(ren.getX() - size.getX()/2);
		int ry = (int)(ren.getY() - size.getY()/2);
		for(int i = 0; i < bimg.getWidth(); i++) {
			for(int j = 0; j < bimg.getHeight(); j++) {
				int rgb = bimg.getRGB(i, j);
				if(((rgb >> 24) & 0xFF) != 0) {
					double r = Math.random() * 8;
					double ang1 = Math.random() * Math.PI * 2;
					double ang2 = Math.random() * Math.PI / 2;
					double vz = r * Math.cos(ang2);
					double vx = r * Math.sin(ang2) * Math.cos(ang1);
					double vy = r * Math.sin(ang2) * Math.sin(ang1);
					int delay = i * 2;
					if(j % 2 == 0) {
						delay = (bimg.getWidth() - i) * 2;
					}
					confetti.add(new Confetti(new Color(bimg.getRGB(i, j)), rx + i, ry + j, 0, vx, vy, vz, delay));
				}
			}
		}
	}
}

class Decision {
	
	Tile type = null;
	boolean locked_direction = false;
	boolean seed = false;
	boolean retrace = false;
	
	ArrayList<Decision> next;
	
	public Decision(Tile type, boolean locked_direction, Decision ... decisions) {
		this.type = type;
		this.locked_direction = locked_direction;
		next = new ArrayList<>();
		for(int i = 0; i < decisions.length; i++) {
			next.add(decisions[i]);
		}
	}
	
	public Decision() {
		this.seed = true;
		this.locked_direction = false;
		this.type = null;
		this.next = new ArrayList<>();
	}
	
	public Decision(Decision d) {
		this.type = d.type;
		this.locked_direction = d.locked_direction;
		this.next = d.next;
		this.retrace = d.retrace;
		this.seed = d.seed;
	}
	
	public void addDecision(Decision d) {
		next.add(d);
	}
	
}

class RealDecision extends Decision {
	
	Point pos;
	
	public RealDecision(Decision d, Point pt) {
		super(d);
		pos = pt;
	}
	
}

class Comment extends Point {
	String comment;
	
	public Comment(String comment, int x, int y) {
		super(x, y);
		this.comment = comment;
	}
	
}

class Confetti {
	Color c, shadow = new Color(0, 0, 0, 20);
	double x, y, z, vx, vy, vz, az = -0.1;
	int delay = 0;
	boolean atRest = false;
	
	public Confetti(Color c, double x, double y, double z, double vx, double vy, double vz, int delay) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.vx = vx;
		this.vy = vy;
		this.vz = vz;
		this.c = c;
		this.delay = delay;
	}
	
	public void draw(Graphics g) {
		g.setColor(shadow);
		g.fillOval((int)x, (int)y, 2, 2);
		g.setColor(c);
		g.fillOval((int)x, (int)(y - z), 2, 2);
	}
	
	public void update() {
		if(atRest) { return; }
		if(delay > 0) {
			delay--;
			return;
		}
		x += vx;
		y += vy;
		z += vz;
		vz += az;
		if(z < 0) {
			z = 0;
			vx = 0;
			vy = 0;
			vz = 0;
			az = 0;
			atRest = true;
		}
	}
}