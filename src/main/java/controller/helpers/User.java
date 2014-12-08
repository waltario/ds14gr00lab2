package controller.helpers;

public class User {

	private String uname = null;
	private String pass = null;
	private long credits;
	private boolean online;

	public User(String uname, String pass, int credits) {

		this.uname = uname;
		this.pass = pass;
		this.credits = credits;
		this.online = false;
	}

	public boolean authenticate(String pass) {

		if (this.pass.equals(pass))
			return true;

		return false;
	}

	public long buyCredits(long credits) {

		this.credits += credits;
		return this.credits;
	}

	public long removeCredits(long credits) {

		this.credits -= credits;
		return this.credits;
	}

	public long getCredits() {
		return credits;
	}

	public String getUname() {
		return uname;
	}

	public String getPass() {
		return pass;
	}

	public boolean isOnline() {
		return online;
	}

	public void setOnline(boolean online) {
		this.online = online;
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();

		sb.append(uname);
		sb.append(" ");
		sb.append(isOnline() ? "online" : "offline");
		sb.append(" ");
		sb.append("Credits: " + credits);

		return sb.toString();
	}
}
