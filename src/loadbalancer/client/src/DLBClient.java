import commands.FindLoadBalancerCommand;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.netty.NettyTransport;
import io.atomix.copycat.client.CopycatClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.time.Instant;
import java.time.OffsetTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

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

		_copycatClient.serializer().register(FindLoadBalancerCommand.class);

		CompletableFuture<CopycatClient> future = _copycatClient.connect(dlbServerAddresses);
		future.join();
	}

	public CopycatClient Inner() { return _copycatClient; }

	public void LBAddress(Address address) { _loadBalancerAddress = address; }

	public Address LBAddress() { return _loadBalancerAddress; }

	public static void main(String[] args) {

		if(args.length != 3 && args.length != 7) {
			System.out.println("Usage: <clsthost> <clsthport> <lbaddr_timeout(ms)>");
			return;
		}

		DLBClient client = new DLBClient(Collections.singleton(
			new Address(args[0], Integer.parseInt(args[1]))
		));

		Semaphore semaphore = new Semaphore(1);
		Scanner input = new Scanner(System.in);

		Integer timeout = Integer.parseInt(args[2]);

		Instant lastLBFind = null;
		Boolean running = true;
		while(running) {
			System.out.println("Enter command:");
			String cmd = input.next();

			String urlPath =  null;

			switch(cmd) {
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

			Instant now = Instant.now();
			// also do a timeout
			if(client.LBAddress() == null || lastLBFind == null || now.isAfter(lastLBFind.plusMillis(timeout))) {
				System.out.println("Looking up load balancer address.");

				semaphore.acquireUninterruptibly();

				lastLBFind = now;

				client.Inner().submit(new FindLoadBalancerCommand()).thenAccept(address -> {
					if (address == null) {
						System.out.println("Returned load balancer address was null.");
						return;
					}

					client.LBAddress(address);

					System.out.printf("Load balancer address: %s:%d\n", address.host(), address.port());

					semaphore.release();
				});
			}

			semaphore.acquireUninterruptibly();
			semaphore.release();

			if (client.LBAddress() == null) {
				System.out.println("Returned address was null");
				continue;
			}

			if(urlPath == null) {
				System.out.println("Command was incorrect.");
				continue;
			}

			HttpURLConnection conn = null;

			URL url = null;
			try {
				url = new URL("http", client.LBAddress().host(), client.LBAddress().port(), urlPath);
			}
			catch (MalformedURLException ex) {
				System.out.printf("Url was malformed: %s\n", ex.getMessage());
				continue;
			}

			try {
				conn = (HttpURLConnection) url.openConnection();
			}
			catch (IOException ex) {
				System.out.printf("Error opening http connection: %s\n", ex.getMessage());
				continue;
			}

			try {
				conn.setRequestMethod("GET");
			}
			catch (ProtocolException ex) {
				System.out.printf("Error setting http request method: %s\n", ex.getMessage());
				continue;
			}

			try {
				conn.connect();

				InputStream resBody = conn.getInputStream();

				byte [] b = new byte[resBody.available()];

				resBody.read(b, 0, resBody.available());

				resBody.close();

				System.out.printf("Server responded with: %s\n", new String(b));
			}
			catch (IOException ex) {
				System.out.printf("Error executing http request: %s", ex.getMessage());
			}
		}
		client.Inner().close();
		return;
	}
}
