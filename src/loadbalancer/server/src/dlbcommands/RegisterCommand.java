package dlbcommands;

import io.atomix.catalyst.transport.Address;
import io.atomix.copycat.Command;

public class RegisterCommand implements Command<Boolean> {
	private String _srvhost = null;
	private Integer _srvport = null;
	private String _lbhost = null;
	private Integer _lbport = null;

	public RegisterCommand(String srvhost, Integer srvport, String lbhost, Integer lbport) {
		_srvhost = srvhost;
		_srvport = srvport;
		_lbhost = lbhost;
		_lbport = lbport;
	}

	public String serverhost() { return _srvhost; }

	public Integer serverport() { return _srvport; }

	public String lbhost() { return _lbhost; }

	public Integer lbport() { return _lbport; }
}
