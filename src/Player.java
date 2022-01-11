
public class Player {
	private BugMenu menu;
	int team;
	boolean playedQueen = false;
	
	public Player(int team) {
		this.team = team;
		menu = new BugMenu(team);
	}
	
	public BugMenu getMenu() {
		return menu;
	}
}
