import io.atomix.catalyst.transport.Address;
import queries.FindLoadBalancerQuery;
import io.atomix.copycat.client.CopycatClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

public class SendRequest implements Runnable
{
	private Integer reqNum;
	private String urlPath;
	private Collection<Address> srvAddrs;
	private CountDownLatch latch;

	public SendRequest(Integer _reqNum, String _urlPath, Collection<Address> _srvAddrs, CountDownLatch _latch)
	{
		this.reqNum = _reqNum;
		this.urlPath = _urlPath;
		this.srvAddrs = _srvAddrs;
		this.latch = _latch;
	}

	@Override
	public void run() {

		DLBClient client = new DLBClient(srvAddrs);

		final Date start = new Date();

		client.Inner().submit(new FindLoadBalancerQuery()).thenAccept(address ->
		{
			if (address == null) {
				System.out.printf("%d: Returned load balancer address was null.\n", reqNum);
				latch.countDown();
				return;
			}

			System.out.printf("%d: Load balancer address: %s:%d\n", reqNum, address.host(), address.port());

			if(urlPath == null) {
				System.out.printf("%d: Command was incorrect.\n", reqNum);
				latch.countDown();
				return;
			}

			HttpURLConnection conn;

			URL url;
			try {
				url = new URL("http", address.host(), address.port(), urlPath);
			}
			catch (MalformedURLException ex) {
				System.out.printf("%d: Url was malformed: %s\n", reqNum, ex.getMessage());
				latch.countDown();
				return;
			}

			try {
				conn = (HttpURLConnection) url.openConnection();
			}
			catch (IOException ex) {
				System.out.printf("%d: Error opening http connection: %s\n", reqNum, ex.getMessage());
				latch.countDown();
				return;
			}

			try {
				conn.setRequestMethod("GET");
			}
			catch (ProtocolException ex) {
				System.out.printf("%d: Error setting http request method: %s\n", reqNum, ex.getMessage());
				latch.countDown();
				return;
			}

			try {

				conn.connect();

				InputStream resBody = conn.getInputStream();

				Date end = new Date();

				byte [] b = new byte[resBody.available()];

				resBody.read(b, 0, resBody.available());

				resBody.close();

				Long runTime = end.getTime() - start.getTime();

				System.out.printf("%d: Server response: %s\n", reqNum, new String(b));
				System.out.printf("%d: Request time: %d\n", reqNum, runTime);
			}
			catch (IOException ex) {
				System.out.printf("%d: Error executing http request: %s", reqNum, ex.getMessage());
			}

			client.Inner().close();
			latch.countDown();
		});
	}
}