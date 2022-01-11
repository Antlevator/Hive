import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class BugMenu {
	static final Color menuColor = new Color(10, 10, 10, 180);
	static final int listWidth = 2, marginX = 10, spacingx = -8, spacingy = 14;
	static double closedWidth = 30, openWidth = 0, width = closedWidth, wspeed = 20, marginTop = 50;
	
	static {
		Point2D size = Tile.getSpriteSize(BugOption.spriteScale);
		openWidth = listWidth * size.getX() + (listWidth+1) * (spacingx) + marginX * 2;
	}
	
	ArrayList<BugOption> options = new ArrayList<>();
	
	public BugMenu(int team) {
		options.add(new BugOption(new Tile(0, 0, team, Tile.BUG_ANT, false), 3));
		options.add(new BugOption(new Tile(0, 0, team, Tile.BUG_GRASSHOPPER, false), 3));
		options.add(new BugOption(new Tile(0, 0, team, Tile.BUG_SPIDER, false), 2));
		options.add(new BugOption(new Tile(0, 0, team, Tile.BUG_BEETLE, false), 2));
		options.add(new BugOption(new Tile(0, 0, team, Tile.BUG_BEE, false), 1));
		options.add(new BugOption(new Tile(0, 0, team, Tile.BUG_LADYBUG, false), 1));
		options.add(new BugOption(new Tile(0, 0, team, Tile.BUG_MOSQUITO, false), 1));
		options.add(new BugOption(new Tile(0, 0, team, Tile.BUG_PILLBUG, false), 1));
		options.add(new BugOption(new Tile(0, 0, team, Tile.BUG_DRAGONFLY, false), 1));
		options.add(new BugOption(new Tile(0, 0, team, Tile.BUG_DUNGBEETLE, false), 1));
		options.add(new BugOption(new Tile(0, 0, team, Tile.BUG_DUNG, false), 99));
		options.add(new BugOption(new Tile(0, 0, team, Tile.BUG_CLINTON, false), 1));
		// TODO add custom bugs here
	}
	
	public BugOption draw(Graphics2D g, int screenWidth, int screenHeight, Point mouse) {
		
		BugOption selection = null;
		
		g.setColor(menuColor);
		g.fillRect(screenWidth - (int)width, 0, screenWidth, screenHeight);
		
		double transx = screenWidth - width;
		
		Point m2 = new Point((int)(mouse.x - transx), mouse.y);
		
		Point2D size = Tile.getSpriteSize(BugOption.spriteScale);
		
		g.translate(transx, 0);
		double x = 0, y = 0;
		g.setFont(g.getFont().deriveFont(18f));
		for(int i = 0; i < options.size(); i++) {
			if(!options.get(i).isEmpty()) {
				options.get(i).disabled = false; // re-enable if necessary
				int lx = i % listWidth;
				int ly = i / listWidth;
				x = marginX + lx * size.getX() + (lx + 1) * (spacingx) + size.getX()/2;
				y = ly * size.getY() + (ly + 1) * spacingy + marginTop + ((lx % 2 != 0)? (size.getY() + spacingy) / 2 : 0);
				boolean sel = options.get(i).draw(g, x, y, m2);
				if(sel) {
					selection = options.get(i);
				}
			} else if(!options.get(i).disabled) {
				// push option to the end of the list, so it doesn't create a weird offset
				options.get(i).disabled = true;
				options.add(options.remove(i));
				i--; // counteract forward motion to not skip anything
			}
		}
		g.translate(-(transx), 0);
		return selection;
	}
	
	public double reactToMouse(Point mouse, int screenWidth) {
		Point m2 = new Point((int)(mouse.x - marginX), mouse.y);
		
		if(hasMouse(m2, screenWidth)) {
			expand();
		} else {
			contract();
		}
		return width - closedWidth;
	}
	
	public boolean hasMouse(Point mouse, int screenWidth) {
		if(screenWidth - width - marginX < mouse.x) {
			return true;
		}
		return false;
	}
	
	private void expand() {
		if(width < openWidth) {
			width += wspeed;
			if(width > openWidth) {
				width = openWidth;
			}
		}
	}
	
	private void contract() {
		if(width > closedWidth) {
			width -= wspeed;
			if(width < closedWidth) {
				width = closedWidth;
			}
		}
	}
	
}

class BugOption {
	
	static double hoverScale = 1.2, spriteScale = 1.5, selectScale = 2;
	
	Tile bug = null;
	int quantity = 1;
	boolean selected = false;
	boolean disabled = false;
	
	public BugOption(Tile bug, int quantity) {
		this.bug = bug;
		this.quantity = quantity;
	}
	
	public boolean draw(Graphics2D g, double x, double y, Point mouse) {
		boolean selected = false;
		BufferedImage bimg = bug.getTileSubimage();
		Point2D size;
		double scale = 1;
		if(hasMouse(x, y, mouse)) {
			selected = true;
			size = Tile.getSpriteSize(selectScale);
			scale = selectScale;
		} else {
			size = Tile.getSpriteSize(spriteScale);
			scale = spriteScale;
		}
		g.translate(x, y);
		bug.drawTileSprite(g, scale);
		g.translate(-size.getX() / 2, -size.getY() / 2);
//		g.drawImage(bimg, 0, 0, (int)size.getX(), (int)size.getY(), null);
		int offsx = 30, offsy = 20;
		g.setColor(Color.black);
		g.drawString("x" + quantity, (int)(size.getX()/2 + offsx)+1, (int)(size.getY()/2 + offsy)+1);
		g.setColor(Color.white);
		g.drawString("x" + quantity, (int)(size.getX()/2 + offsx), (int)(size.getY()/2 + offsy));
		g.translate(size.getX() / 2, size.getY() / 2);
		g.translate(-x, -y);
		return selected;
	}
	
	public void decrement() {
		quantity--;
	}
	
	public boolean isEmpty() {
		return quantity <= 0;
	}
	
	public boolean hasMouse(double x, double y, Point mouse) {
		Point2D size = Tile.getSpriteSize(hoverScale);
		x -= size.getX() / 2;
		y -= size.getY() / 2;
		if(mouse.x > x && mouse.x < x + size.getX() && mouse.y > y && mouse.y < y + size.getY()) {
			return true;
		}
		return false;
	}

}