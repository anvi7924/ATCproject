import queries.FindLoadBalancerQuery;
import dlbcommands.RegisterCommand;
import dlbcommands.RemoveCommand;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.netty.NettyTransport;
import io.atomix.copycat.client.CopycatClient;
import io.atomix.copycat.server.CopycatServer;
import io.atomix.copycat.server.storage.Storage;
import io.atomix.copycat.server.storage.StorageLevel;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class DLBServer {
	private CopycatClient _copycatClient = null;
	private CopycatServer _copycatServer = null;

	public DLBServer(Address address, Address lbAddress, String dirPrefix)
	{
		initServer(address, dirPrefix);

		if(_copycatServer != null)
		{
			CompletableFuture<CopycatServer> serverFuture = _copycatServer.bootstrap();
			serverFuture.join();
		}

		System.out.println("Connected server");

		initClient();

		if(_copycatClient != null)
		{
			CompletableFuture<CopycatClient> future = _copycatClient.connect(address);
			future.join();
		}

		System.out.println("Connected client");

		initState(address, lbAddress);
	}

	public DLBServer(Address address, Collection<Address> clusterAddresses, Address lbAddress, String dirPrefix)
	{
		initServer(address, dirPrefix);

		if(_copycatServer != null)
			_copycatServer.join(clusterAddresses).join();

		initClient();

		if(_copycatClient != null)
		{
			CompletableFuture<CopycatClient> future = _copycatClient.connect(clusterAddresses);
			future.join();
		}

		initState(address, lbAddress);
	}

	private void initServer(Address address, String dirPrefix)
	{
		// start up server
		_copycatServer = CopycatServer.builder(address)
				.withStateMachine(DLBStateMachine::new)
				.withTransport(NettyTransport.builder()
						.withThreads(4)
						.build())
				.withStorage(Storage.builder()
						.withDirectory(new File(dirPrefix + "_log"))
						.withStorageLevel(StorageLevel.DISK)
						.build())
				.build();

		_copycatServer.serializer().register(FindLoadBalancerQuery.class);
		_copycatServer.serializer().register(RegisterCommand.class);
		_copycatServer.serializer().register(RemoveCommand.class);

		System.out.println("Initialized server");
	}

	private void initClient()
	{
		// start up client
		_copycatClient = CopycatClient.builder()
				.withTransport(NettyTransport.builder()
						.withThreads(2)
						.build())
				.build();

		_copycatClient.serializer().register(RegisterCommand.class);
		_copycatClient.serializer().register(RemoveCommand.class);

		System.out.println("Initialized client");
	}

	private void initState (Address srvAddr, Address lbAddr)
	{
		if(_copycatClient != null) {
			// add this server to the replicated list
			_copycatClient.submit(new RegisterCommand(
				srvAddr.host(),
				srvAddr.port(),
				lbAddr.host(),
				lbAddr.port()
				));
		}

		if(_copycatServer != null) {
			// set up listener for member removal
			_copycatServer.cluster().onLeave(member -> {
				Address memAddr = member.address();
				_copycatClient.submit(new RemoveCommand(
					memAddr.host(),
					memAddr.port()
				));
			});
		}

		System.out.println("Initialized state");
	}

	public CopycatClient InnerClient() { return _copycatClient; }

	public CopycatServer InnerServer() { return _copycatServer; }

	public static void main(String[] args) {
		if(args.length != 5 && args.length != 7) {
			System.out.println("Usage: <srvhost> <srvport> [<clsthost> <clstport>] <lbhost> <lbport> <logdir>");
			return;
		}

		Address srvAddr = new Address(args[0], Integer.parseInt(args[1]));

		if(args.length == 5)
		{
			Address lbAddr = new Address(args[2], Integer.parseInt(args[3]));
			DLBServer server = new DLBServer(srvAddr, lbAddr, args[4]);
		}
		else
		{
			Address clstAddr = new Address(args[2], Integer.parseInt(args[3]));
			Address lbAddr = new Address(args[4], Integer.parseInt(args[5]));
			DLBServer server = new DLBServer(srvAddr, Collections.singleton(clstAddr), lbAddr, args[6]);
		}
	}
}
