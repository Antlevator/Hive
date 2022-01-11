import java.awt.Point;
import java.awt.geom.Point2D;

public class Beetle extends Tile {
	
	public Beetle(int lox, int loy, int team) {
		super(lox, loy, team);
		this.bug = BUG_BEETLE;
	}
	
	public boolean attemptMoveOnEmpty(Point2D pt) {
		if(Math.abs(pt.getX() - lox) > 1 || Math.abs(pt.getY() - loy) > 1) {
			return false;
		}
		if(getRenderRise() == 0 && checkIfSqueezing(toPoint(), new Point((int)pt.getX(), (int)pt.getY()))) {
			System.out.println("SQUEEZING");
			return false;
		}
		changePositionInMap((int)pt.getX(), (int)pt.getY());
		return true;
	}
	
	public boolean attemptMoveOnTile(Tile target) {
		if(Math.abs(target.lox - lox) > 1 || Math.abs(target.loy - loy) > 1) {
			return false;
		}
		changePositionInMap(target.lox, target.loy);
		return true;
	}

}
