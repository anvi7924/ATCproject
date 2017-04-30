import queries.FindLoadBalancerQuery;
import dlbcommands.RegisterCommand;
import dlbcommands.RemoveCommand;
import io.atomix.catalyst.transport.Address;
import io.atomix.copycat.server.Commit;
import io.atomix.copycat.server.Snapshottable;
import io.atomix.copycat.server.StateMachine;
import io.atomix.copycat.server.storage.snapshot.SnapshotReader;
import io.atomix.copycat.server.storage.snapshot.SnapshotWriter;

import java.util.Random;

public class DLBStateMachine extends StateMachine implements Snapshottable {
	private DLBState state = new DLBState();

	@Override
	public void snapshot(SnapshotWriter snapshotWriter) {
		snapshotWriter.writeObject(state);
	}

	@Override
	public void install(SnapshotReader snapshotReader) {
		state = snapshotReader.readObject();
	}

	public Boolean Register(Commit<RegisterCommand> commit)
	{
		try {
			String srvhost = commit.operation().serverhost();
			Integer srvport = commit.operation().serverport();
			String lbhost = commit.operation().lbhost();
			Integer lbport = commit.operation().lbport();

			Address srvAddr = new Address(srvhost, srvport);
			Address lbAddr = new Address(lbhost, lbport);

			if(state.loadBalancers.containsKey(srvAddr)) {
				System.out.printf("Found address %s:%d in list, not adding\n", lbAddr.host(), lbAddr.port());
				return true;
			}

			System.out.printf("Adding address %s:%d to list\n", lbAddr.host(), lbAddr.port());

			state.loadBalancers.put(srvAddr, lbAddr);

			state.keyList.add(srvAddr);

			return true;
		}
		catch (Exception ex) {
			return false;
		}
		finally {
			commit.close();
		}
	}

	public Boolean Remove(Commit<RemoveCommand> commit)
	{
		try {
			String host = commit.operation().host();
			Integer port = commit.operation().port();
			System.out.printf("Handling remove command for address %s:%d\n", host, port);

			Address srvAddr = new Address(host, port);

			state.loadBalancers.remove(srvAddr);

			state.keyList.remove(srvAddr);

			return true;
		}
		catch (Exception ex) {
			return false;
		}
		finally {
			commit.close();
		}
	}

	public Address FindLoadBalancer(Commit<FindLoadBalancerQuery> commit)
	{
		try {
			System.out.println("Handling find load balancer query.");

			Random rand = new Random();
			Integer randOffset = rand.nextInt(state.keyList.size());

			Address key = state.keyList.get(randOffset);

			return state.loadBalancers.get(key);
		}
		catch (Exception ex) {
			return null;
		}
		finally {
			commit.close();
		}
	}
}
