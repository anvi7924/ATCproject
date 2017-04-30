import queries.FindLoadBalancerQuery;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.netty.NettyTransport;
import io.atomix.copycat.client.CopycatClient;

import java.util.Collection;
import java.util.Collections;
import java.util.Scanner;
import java.util.concurrent.*;

public class DLBClient
{
	private CopycatClient _copycatClient = null;
	private Address _loadBalancerAddress = null;

	public DLBClient(Collection<Address> dlbServerAddresses)
	{
		_loadBalancerAddress = null;

		_copycatClient = CopycatClient.builder()
				.withTransport(NettyTransport.builder()
						.withThreads(2)
						.build())
				.build();

		_copycatClient.serializer().register(FindLoadBalancerQuery.class);

		CompletableFuture<CopycatClient> future = _copycatClient.connect(dlbServerAddresses);
		future.join();
	}

	public CopycatClient Inner() { return _copycatClient; }

	public void LBAddress(Address address) { _loadBalancerAddress = address; }

	public Address LBAddress() { return _loadBalancerAddress; }

	public static void main(String[] args) {

		if(args.length != 3 && args.length != 7) {
			System.out.println("Usage: <clsthost> <clsthport> <number of thread>");
			return;
		}

		Scanner input = new Scanner(System.in);

		Boolean running = true;
		while(running) {

			System.out.println("Enter command:");
			String cmd = input.next();

			String urlPath = null;

			switch (cmd) {
				case "l":
					urlPath = "/light";
					break;
				case "m":
					urlPath = "/medium";
					break;
				case "h":
					urlPath = "/heavy";
					break;
				case "q":
					running = false;
					continue;
				default:
					break;
			}

			Integer numThreads = Integer.parseInt(args[2]);

			CountDownLatch latch = new CountDownLatch(numThreads);
			ExecutorService threads = Executors.newFixedThreadPool(numThreads);

			for (int i = 0; i < numThreads; i++) {
				threads.execute(new SendRequest(i, urlPath,
					Collections.singleton(new Address(args[0], Integer.parseInt(args[1]))),
					latch));
			}

			try {
				latch.await();
			} catch (InterruptedException ex) {
				System.out.printf("Error sending the requests: %s\n", ex.getMessage());
			}

			System.out.println();
		}
	}
}
