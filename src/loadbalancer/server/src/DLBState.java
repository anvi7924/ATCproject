import io.atomix.catalyst.transport.Address;

import java.util.ArrayList;
import java.util.HashMap;

public class DLBState
{
	public ArrayList<Address> keyList;
	public HashMap<Address, Address> loadBalancers;

	public DLBState()
	{
		keyList = new ArrayList<>();
		loadBalancers = new HashMap<>();
	}
}
