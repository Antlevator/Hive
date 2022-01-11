
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import java.util.Comparator;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.WindowConstants;

public class Main {
	
	static Point mouse = new Point(0, 0), press = null, click = null;
	static int shiftx, shifty;
	static Tile selected = null, target_of_selected = null;
	static ArrayList<Point> path_of_selected = null;
	static double oscillator = 0;
	static boolean inTransit = false, inMenu = false;
	static double selx = -1, sely = -1, selh = 0, duration = 7, startval = -1;
	static int turns = 0;
	static BugOption menuSelection = null;
	
	static int NUM_PLAYERS = 2;
	
	static ArrayList<Player> players = new ArrayList<>();
	static int movect = 0;
	
	static ArrayList<Confetti> confetti = new ArrayList<>();
	
	static boolean complete = false;
	static Player winner = null;
	
	static boolean done_picking = false;
	static float sat = 0.5f;
	static Color picked;
	
	public static void main(String[] args) {
		
		boolean validNumber = false;
		while(!validNumber) {
			validNumber = true;
			String num = JOptionPane.showInputDialog("Please enter the number of players");
			try {
				NUM_PLAYERS = Integer.parseInt(num);
				if(NUM_PLAYERS <= 0) {
					validNumber = false;
				}
			} catch(NumberFormatException e) {
				validNumber = false;
			}
		}
		
		boolean loop_until_complete = true;
		while(loop_until_complete) {
			
			if(players.size() >= NUM_PLAYERS) {
				loop_until_complete = false;
			} else {
				picked = null;
				done_picking = false;
				sat = 0.5f;
				getColor();
			}
			
			while(!done_picking) { System.out.print(""); }
			
			if(picked != null) {
				players.add(new Player(picked.getRGB()));
			}
			
			if(players.size() == NUM_PLAYERS) {
				break;
			}
			
		}
		
		runGame();
		
	}
	
	public static void getColor() {
		
//		done_picking = false;
//		picked = null;
//		sat = 0.5f;
		
		JFrame colorframe = new JFrame();
		colorframe.setSize(500, 300);
		colorframe.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		
		int briWidth = 20;
		int viewWidth = 40;
		
		JPanel colorpanel = new JPanel() {
			@Override
			public void paintComponent(Graphics g) {
				
				int pickWidth = getWidth() - viewWidth - briWidth;
				
				for(int j = 0; j < getHeight(); j++) {
					g.setColor(Color.getHSBColor(0.0f, 0.0f, j / (float)getHeight()));
					g.fillRect(pickWidth, j, briWidth, 2);
				}
				
				g.setColor(Color.getHSBColor(0.0f, 1.0f, 1 - sat));
				g.drawLine(pickWidth, (int)(sat * getHeight()), pickWidth + briWidth, (int)(sat * getHeight()));
				
				if(picked != null) {
					g.setColor(picked);
				} else {
					g.setColor(Color.black);
				}
				g.fillRect(pickWidth + briWidth, 0, viewWidth, getHeight());
				
				int cellsize = 2;
				for(int i = 0; i < pickWidth; i += cellsize) {
					for(int j = 0; j < getHeight(); j += cellsize) {
						g.setColor(Color.getHSBColor(i / (float)pickWidth, sat, j / (float)getHeight()));
						g.fillRect(i, j, cellsize, cellsize);
					}
				}
			}
		};
		
		MouseAdapter ma = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				mousefunc(e);
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
				mousefunc(e);
			}
			
			public void mousefunc(MouseEvent e) {
				int pickw = colorpanel.getWidth() - viewWidth - briWidth;
				
				int i = e.getPoint().x;
				int j = e.getPoint().y;
				
				if(j >= 0 && j < colorpanel.getHeight()) {
					if(i > pickw && i < pickw + briWidth) {
						sat = j / (float)colorpanel.getHeight();
						if(picked != null) {
							float[] fl = new float[3];
							Color.RGBtoHSB(picked.getRed(), picked.getGreen(), picked.getBlue(), fl);
							picked = Color.getHSBColor(fl[0], sat, fl[2]);
						}
					} else if(i < pickw) {
						picked = Color.getHSBColor(i / (float)pickw, sat, j / (float)colorpanel.getHeight());
					}
					colorframe.repaint();
				}
			}
		};
		
		Action confirmAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(picked != null) {
					done_picking = true;
					colorframe.dispose();
				}
			}
		};
		
		colorpanel.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), "confirm");
		colorpanel.getActionMap().put("confirm", confirmAction);
		
		colorpanel.addMouseListener(ma);
		colorpanel.addMouseMotionListener(ma);
		
		colorframe.add(colorpanel);
		
		colorframe.setTitle("Choose a Tile Color");
		colorframe.setResizable(false);
		colorframe.setVisible(true);
	
	}
	
	public static void runGame() {
		
		JFrame frame = new JFrame();
		frame.setSize(800, 800);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		
//		players.add(new Player(0xc9d519));
//		players.add(new Player(0x463c30));
//		players.add(new Player(0xBEEEEF));
		
//		for(int i = 0; i < 10; i++) {
//			for(int j = 0; j < 10; j++) {
//				int color = 0;
//				if(i % 2 == j % 2) {
//					color = 0xFF00FFFF;
//				}
//				int bug = Tile.BUG_MOSQUITO_AS_BEETLE;
//				while(bug == Tile.BUG_MOSQUITO_AS_BEETLE) {
//					bug = (int)(Math.random() * 10);
//				}
//				Tile t = new Tile(i, j, color, bug);
//			}
//		}
		
		Random r = new Random(System.currentTimeMillis());
		Color ground = Color.getHSBColor(r.nextFloat() * Float.MAX_VALUE, 0.2f, 0.7f);
		
		JPanel panel = new JPanel() {
			@Override
			public void paintComponent(Graphics go) {
				Graphics2D g = (Graphics2D) go.create();
				
				oscillator += 1;
				
				g.setColor(ground);
				g.fillRect(0, 0, getWidth(), getHeight());
				
				double tempshx = shiftx, tempshy = shifty;
				if(press != null) {
					tempshx += mouse.getX() - press.getX();
					tempshy += mouse.getY() - press.getY();
				}
				
				Point m2 = new Point((int)(mouse.getX() - tempshx), (int)(mouse.getY() - tempshy));
				Point mclick = null;
				if(click != null) {
					mclick = new Point((int)(click.getX() - tempshx), (int)(click.getY() - tempshy));
				}
				g.setColor(Color.black);
				
//				if(press != null) {
//					g.drawString("Press", press.x, press.y);
//				}
//				g.drawString("Mouse", mouse.x, mouse.y);
				
				if(!players.isEmpty()) {
					players.get(movect % players.size()).getMenu().reactToMouse(mouse, getWidth());
					inMenu = players.get(movect % players.size()).getMenu().hasMouse(mouse, getWidth());
				}
				
				g.translate(tempshx, tempshy);
				boolean selected_this_frame = false; // prevent selection and targeting in same frame
				Iterator<Point> iterator = Tile.board.keySet().iterator();
				ArrayList<Point> orderedCollection = new ArrayList<Point>(Tile.board.size());
				while(iterator.hasNext()) {
					Point next = iterator.next();
					int idx = Collections.binarySearch(orderedCollection, next, new HexComparator());
					if(idx < 0) {
						idx = -(idx + 1);
						orderedCollection.add(idx, next);
					}
				}
				
				for(Point entry : orderedCollection) {
					
					ArrayList<Tile> value = Tile.board.get(entry);
					assert(value != null);
					int iter = 0;
					ArrayList<Tile> ts = value;
					for(int i = 0; i < ts.size(); i++) {
						Tile t = ts.get(i);
						iter++;
						if(!inTransit || !t.equals(selected)) {
							t.draw(g, m2);
						}
						if(!complete && !inTransit && !inMenu && menuSelection == null) {
							if(iter == ts.size() && mclick != null) {
								if(selected == null) {
									if(t.select(mclick)) {
										boolean team_conflict = t.team != players.get(movect % players.size()).team;
										System.out.println("TILE SELECTED: " + t + " FROM ENTRY " + entry + " on iteration " + iter);
										// if players must play queen at this point
										if(players.get(movect % players.size()).playedQueen == true || movect / players.size() < 2) {
											// if tile can be moved at all
											if(Tile.canMoveWithoutBreakingHive(t.toPoint())) {
												selected = t;
												selected.findPossibleMoves(players.get(movect % players.size()).team);
												assert(selected.possibleMoves != null);
												selected_this_frame = true;
											}
										}
										click = null;
									}
								} else if(selected != null && target_of_selected == null) {
									if(t.team == players.get(movect % players.size()).team && t.select(mclick) && Tile.anyPathHasEndpoint(selected.possibleMoves, t.toPoint())) {
										System.out.println("TARGET SELECTED: " + t + " FROM ENTRY " + entry + " on iteration " + iter);
										target_of_selected = t;
									}
								}
							}
						}
					}
				}
				
				// placing selected piece from menu onto the board
				if(!complete && !inTransit && !inMenu && menuSelection != null && click != null && m2 != null) {
					Tile selcopy = menuSelection.bug.copy();
					Vector pv = Tile.getLogicalHexGridCoordinate(m2);
					Point2D pt = pv.toPoint2D();
					Point pt2 = new Point((int)pt.getX(), (int)pt.getY());
					selcopy.lox = pt2.x;
					selcopy.loy = pt2.y;
					System.out.println("Attempting to place at position (" + selcopy.lox + ", " + selcopy.loy + ")");
					
					boolean viablePosition = true;
					
					// if players must play queen at this point
					if(players.get(movect % players.size()).playedQueen == false) {
						if(movect / players.size() >= 2) {
							if(selcopy.bug != Tile.BUG_BEE) {
								viablePosition = false;
							}
						}
					}
					
					if(Tile.board.get(pt) != null) {
						viablePosition = false;
						System.out.println("Cant place on top of the hive!");
					} else {
						
						if(selcopy.bug != Tile.BUG_DUNG) {
							Point[] nbrs = Tile.getNeighborTiles(pt2);
							
							int ct = 0;
							for(int i = 0; i < nbrs.length; i++) {
								ArrayList<Tile> nbrstack = Tile.board.get(nbrs[i]);
								if(nbrstack != null && nbrstack.size() > 0) {
									ct++;
									// if there is a team disagreement among neighbors, we can't place the tile here
									if(nbrstack.get(nbrstack.size() - 1).team != selcopy.team) {
										// make sure this isn't in the first n turns for the n players, as those can touch other colors
										if(movect >= players.size()) {
											System.out.println("Neighbor color does not match color of tile to be placed!");
											viablePosition = false;
										}
										break;
									}
								}
							}
							
							if(ct == 0 && !Tile.board.isEmpty()) {
								System.out.println("No neighbors! Must connect to hive");
								viablePosition = false;
							}
						}
					}
					
					if(viablePosition && menuSelection.bug.bug == Tile.BUG_DUNG) {
						viablePosition = false;
						Point[] nbrs = Tile.getNeighborTiles(pt2);
						for(int i = 0; i < nbrs.length; i++) {
							if(nbrs[i] != null) {
								ArrayList<Tile> nbrstack = Tile.board.get(nbrs[i]);
								if(nbrstack != null) {
									Tile top = nbrstack.get(nbrstack.size() - 1);
									if(top.bug == Tile.BUG_DUNGBEETLE && top.team == menuSelection.bug.team) {
										viablePosition = true;
									}
									if(top.bug == Tile.BUG_MOSQUITO && top.team == menuSelection.bug.team) {
										if(Tile.hasNeighborOfTypeAndTeam(top.toPoint(), Tile.BUG_DUNGBEETLE, menuSelection.bug.team)) {
											viablePosition = true;
										}
									}
								}
							}
						}
					}
					
					if(viablePosition) {
						selcopy.setPositionInMap();
						System.out.println("Set position to (" + selcopy.lox + ", " + selcopy.loy + ")");
						menuSelection.decrement(); // use up the quantity
						if(menuSelection.bug.bug == Tile.BUG_BEE) {
							players.get(movect % players.size()).playedQueen = true;
						}
						movect++;
						checkForQueenSurrounded(); // can only occur due to meatballs
					}
					menuSelection = null;
					click = null;
					mclick = null;
				}
				
				if(!complete && !inTransit && !inMenu && menuSelection == null) {
					boolean selectedEmptySpace = false;
					// Player is attempting to selected empty space, which is valid
					if(selected != null && !selected_this_frame && target_of_selected == null && mclick != null) {
						assert(selected.possibleMoves != null);
						if(Tile.anyPathHasEndpoint(selected.possibleMoves, Tile.getLogicalHexGridCoordinate(mclick).toPoint())) {
							selectedEmptySpace = true;
							System.out.println("EMPTY SELECTED");
						// clicked on something that was invalid, deselect current piece
						} else {
							selected = null;
							click = null;
						}
					}
					
					// player has selected another piece, this is true for stacking bugs like the beetle
					if(selected != null && (target_of_selected != null || selectedEmptySpace)) {
						if(selectedEmptySpace) {
							System.out.println("HANDLING EMPTY SELECTION");
							Vector p = Tile.getLogicalHexGridCoordinate(m2);
							Point2D pt = p.toPoint2D();
							selected.attemptMove(pt);
						} else {
							if(!selected.equals(target_of_selected)) {
								System.out.println("HANDLING TARGET SELECTION");
								selected.attemptMove(target_of_selected.toPoint());
							}
						}
						
						// animation
						inTransit = true;
						movect++;
						startval = oscillator;
						if(selectedEmptySpace) {
							path_of_selected = Tile.convertPathToPoints(Tile.getPathWithEndpoint(selected.possibleMoves, Tile.getLogicalHexGridCoordinate(m2).toPoint()));
						} else {
							path_of_selected = Tile.convertPathToPoints(Tile.getPathWithEndpoint(selected.possibleMoves, target_of_selected.toPoint()));
						}
						
						selectedEmptySpace = false;
						click = null;
						target_of_selected = null;
								
						System.out.println("HANDLING MOVE COMPLETE");
					}
					
					if(selected != null) {
						selected.drawReachables(g);
					}
					
					if(selected != null) {
						g.setColor(new Color(0, 255, 0, 127));
						g.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
						Tile.drawHexAtLogicalCoordinate(g, selected.toPoint(), false, Math.cos(oscillator/5)*.1 + 1);
					}
				
				}
				
				if(!complete && !inTransit && !inMenu) {
					g.setColor(new Color(players.get(movect % players.size()).team));
					Tile.drawHexAt(g, m2, false);
				}
				
				if(inTransit) {
					
					if(path_of_selected != null && path_of_selected.size() > 1) {
						
						boolean last = path_of_selected.size() == 2;
						
						Point a = path_of_selected.get(0);
						double ah = 0;
						ArrayList<Tile> apos = Tile.board.get(a);
						if(apos != null) {
							ah = apos.size() * Tile.rise;
						}
						
						Point b = path_of_selected.get(1);
						double bh = 0;
						ArrayList<Tile> bpos = Tile.board.get(b);
						if(bpos != null) {
							bh = (bpos.size() - (last?1:0)) * Tile.rise;
						}
						
						double ratio = (oscillator - startval) / duration;
						selx = b.x * ratio + a.x * (1 - ratio);
						sely = b.y * ratio + a.y * (1 - ratio);
						selh = bh * ratio + ah * (1 - ratio);
					}
					
					if(selected != null) {
						Point2D renpt = Tile.getRenderCoordinate(new Point2D.Double(selx, sely));
						g.translate(renpt.getX(), renpt.getY() - selh);
						selected.drawTileSprite(g, 1);
						g.translate(-(renpt.getX()), -(renpt.getY() - selh));
					}
					
					if(oscillator - startval >= duration) {
						if(path_of_selected.size() == 1) {
							selected.lox = path_of_selected.get(0).x;
							selected.loy = path_of_selected.get(0).y;
							ArrayList<Tile> stack = Tile.board.get(selected.toPoint()); // TODO clinton
							if(path_of_selected.get(0).equals(path_of_selected.get(path_of_selected.size() - 1)) && stack != null && selected.bug == Tile.BUG_CLINTON) {
								for(Tile t : stack) {
									if(t.bug != Tile.BUG_BEE) {
										t.bug = Tile.BUG_CLINTON;
										t.team = selected.team;
									}
								}
							}
							selected = null;
							startval = -1;
							inTransit = false;
							path_of_selected = null;
							checkForQueenSurrounded();
						} else {
							path_of_selected.remove(0);
							if(!path_of_selected.isEmpty()) {
								startval = oscillator;
							}
						}
					}
				}
				
				for(int i = confetti.size() - 1; i >= 0; i--) {
					confetti.get(i).draw(g);
					confetti.get(i).update();
					if(confetti.get(i).atRest) {
						confetti.remove(i);
					}
				}
				
				Graphics2D ghud = (Graphics2D) go.create();
				
				BugOption sel = null;
				if(!players.isEmpty()) {
					sel = players.get(movect % players.size()).getMenu().draw(ghud, getWidth(), getHeight(), mouse);
				}
				if(!complete && inMenu && click != null && sel != null) {
					menuSelection = sel;
					selected = null;
					selected_this_frame = false;
					target_of_selected = null;
					path_of_selected = null;
					startval = -1;
					click = null;
				}
				
				if(!complete && menuSelection != null) {
					ghud.translate(mouse.x, mouse.y);
					menuSelection.bug.drawTileSprite(ghud, 1);
					ghud.translate(-mouse.x, -mouse.y);
				}
				
				if(complete) {
					ghud.setFont(ghud.getFont().deriveFont(64f));
					FontMetrics fm = ghud.getFontMetrics();
					int height = fm.getHeight();
					String str = "ERROR!";
					int len = fm.stringWidth(str);
					int color = 0xFFFFFFFF;
					if(players.isEmpty()) {
						str = "DRAW!";
						len = fm.stringWidth(str);
					} else if(players.size() == 1) {
						color = players.get(0).team;
						str = "WINNER!";
						len = fm.stringWidth(str);
					}
					
					ghud.setColor(new Color(~color & 0xFF000000));
					int offs = 4;
					ghud.drawString(str, getWidth()/2 - len/2 + offs, getHeight()/2 + height/2 + offs);
					ghud.setColor(new Color(color));
					ghud.drawString(str, getWidth()/2 - len/2, getHeight()/2 + height/2);
					
				}
				
				ghud.dispose();
				g.dispose();
				click = null;
			}

			private void checkForQueenSurrounded() {
				Iterator<Point> iterator = Tile.board.keySet().iterator();
				ArrayList<Integer> playersToRemove = new ArrayList<>();
				while(iterator.hasNext()) {
					Point pt = iterator.next();
					ArrayList<Tile> stack = Tile.board.get(pt);
					if(stack != null && stack.get(0).bug == Tile.BUG_BEE) {
						int nbrs = Tile.getNeighborTileCount(pt);
						if(nbrs == 6) {
							int bee_team = stack.get(0).team;
//							stack.get(0).removeFromMap();
							stack.get(0).getConfetti(confetti);
							playersToRemove.add(bee_team);
						}
					}
				}
				
				for(Integer team : playersToRemove) {
					for(int i = 0; i < players.size(); i++) {
						if(players.get(i).team == team) {
							players.remove(i);
							break;
						}
					}
					// remove tiles, or if not possible, remove typing of bugs belonging to this team
					generify(team);
				}
				
				if(players.size() == 0) {
					complete = true;
					winner = null;
				} else if(players.size() == 1) {
					complete = true;
					winner = players.get(0);
				}
			}
			
			private void generify(int bteam) {
				ArrayList<Point> keys = new ArrayList<>();
				Iterator<Point> iterator = Tile.board.keySet().iterator();
				while(iterator.hasNext()) {
					keys.add(iterator.next());
				}
				
				for(Point pt : keys) {
					ArrayList<Tile> stack = Tile.board.get(pt);
					if(stack != null) {
						System.out.println("Examining stack at (" + pt.x + ", " + pt.y + ") of size " + stack.size());
						// remove top to bottom, then generify when hive would be broken
						for(int i = stack.size() - 1; i >= 0; i--) {
							System.out.println("Tile " + i);
							
							if(stack.isEmpty()) { 
								System.out.println("Short circuit, stack is empty!");
								break;
							}
							if(stack.get(i).team == bteam) {
								System.out.println("This tile is of the losing team");
								// if this is the last one, it might disconnect the hive
								if(stack.size() == 1) {
									System.out.println("This tile is on the bottom, might disconnect hive");

									if(Tile.canMoveWithoutBreakingHive(pt)) {
										System.out.println("Wont disconnect the hive, removing");
										// because positions are cleaned when empty, the ArrayList may change so go fetch it again
										Tile.board.remove(pt);
									} else {
										// fetch again because removal and replacement can generate a new list
										stack = Tile.board.get(pt);
										System.out.println("Will disconnect the hive");
										stack.get(i).bug = Tile.BUG_NONE;
										stack.get(i).mosquito_mask = Tile.BUG_NONE;
										stack.get(i).possibleMoves.clear();
									}
								} else {
									System.out.println("Can't disconnect hive because the stack is not size 1 and not sandwiched");
									stack.get(i).removeFromMap();
								}
							}
						}
					}
				}
			}
		};
		
		frame.setTitle("Virtual Hive");
		
		MouseAdapter ma = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				press = e.getPoint();
				click = null;
			}
			@Override
			public void mouseReleased(MouseEvent e) {
//				double distance = Math.min(Math.abs(mouse.getX() - press.getX()), Math.abs(mouse.getY() - press.getY()));
//				if(distance < 2) {
//					click = e.getPoint();
//				} else {
//					shiftx += mouse.getX() - press.getX();
//					shifty += mouse.getY() - press.getY();
//				}
				
				if(!mouse.equals(press)) {
					double distance = Math.min(Math.abs(mouse.getX() - press.getX()), Math.abs(mouse.getY() - press.getY()));
					if(distance < 2) {
						click = e.getPoint();
					}
					shiftx += mouse.getX() - press.getX();
					shifty += mouse.getY() - press.getY();
				} else {
					click = e.getPoint();
				}
				press = null;
			}
			@Override
			public void mouseMoved(MouseEvent e) {
				mouse = e.getPoint();
			}
			@Override
			public void mouseDragged(MouseEvent e) {
				mouse = e.getPoint();
			}
			
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				turns += (int)e.getPreciseWheelRotation();
			}
		};
		
		panel.addMouseListener(ma);
		panel.addMouseMotionListener(ma);
		panel.addMouseWheelListener(ma);
		
		frame.add(panel);
		frame.setVisible(true);
		
		Action repaintAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				frame.repaint();
			}
		};
		
		Timer t = new Timer(16, repaintAction);
		t.start();
	}
}

class HexComparator implements Comparator<Point> {

	@Override
	public int compare(Point h1, Point h2) {
		if(h1.getY() != h2.getY()) {
			return (int)Math.signum(h2.getY() - h1.getY());
		} else if(h1.getX() != h2.getX()) {
			return (int)Math.signum(h1.getX() - h2.getX());
		} else {
			return 1;
		}
	}
	
}