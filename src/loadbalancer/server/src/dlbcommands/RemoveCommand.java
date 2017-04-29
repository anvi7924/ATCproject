package dlbcommands;

import io.atomix.catalyst.transport.Address;
import io.atomix.copycat.Command;

public class RemoveCommand implements Command<Boolean> {
	private String _host = null;
	private Integer _port = null;

	public RemoveCommand(String host, Integer port) {
		_host = host;
		_port = port;
	}

	public String host() { return _host; }

	public Integer port() { return _port; }
}
