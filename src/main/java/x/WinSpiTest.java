package x;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class WinSpiTest {
	public static void main(String[] args) throws UnknownHostException {
		IntByReference size = new IntByReference(0);
		int error = IPHlpAPI.INSTANCE.GetAdaptersAddresses(
			IPHlpAPI.AF_UNSPEC,
			IPHlpAPI.GAA_FLAG_SKIP_UNICAST
				| IPHlpAPI.GAA_FLAG_SKIP_ANYCAST
				| IPHlpAPI.GAA_FLAG_SKIP_MULTICAST,
			null,
			null,
			size
		);
		if (error != WinError.ERROR_BUFFER_OVERFLOW) {
			System.exit(1);
		}
		System.out.println(size.getValue());
		Memory buffer = new Memory(size.getValue());
		error = IPHlpAPI.INSTANCE.GetAdaptersAddresses(
			IPHlpAPI.AF_UNSPEC,
			IPHlpAPI.GAA_FLAG_SKIP_UNICAST
				| IPHlpAPI.GAA_FLAG_SKIP_ANYCAST
				| IPHlpAPI.GAA_FLAG_SKIP_MULTICAST,
			Pointer.NULL,
			buffer,
			size
		);
		if (error != WinError.ERROR_SUCCESS) {
			System.exit(1);
		}
		IPHlpAPI.IP_ADAPTER_ADDRESSES_LH result = new IPHlpAPI.IP_ADAPTER_ADDRESSES_LH(buffer);
		do {
			System.out.print(result.IfIndex);
			System.out.println(": -------------------");
			System.out.println(result.AdapterName);
			System.out.println(result.FriendlyName);
			System.out.println(result.DnsSuffix);
			IPHlpAPI.IP_ADAPTER_DNS_SERVER_ADDRESS_XP dns = result.FirstDnsServerAddress;
			while (dns != null) {
				System.out.println(dns.Length + " -> " + dns.Address.iSockaddrLength);
				System.out.println(dns.Address.toAddress());
				dns = dns.Next;
			}
			IPHlpAPI.IP_ADAPTER_DNS_SUFFIX suffix = result.FirstDnsSuffix;
			while (suffix != null) {
				System.out.println(suffix);
				suffix = suffix.Next;
			}
			result = result.Next;
		} while (result != null);
	}

	public interface IPHlpAPI extends Library {
		IPHlpAPI INSTANCE = Native.load("IPHlpAPI", IPHlpAPI.class, W32APIOptions.ASCII_OPTIONS);

		public static int AF_UNSPEC = 0;
		public static int AF_INET = 2;
		public static int AF_INET6 = 23;

		public static int GAA_FLAG_SKIP_UNICAST = 0x0001;
		public static int GAA_FLAG_SKIP_ANYCAST = 0x0002;
		public static int GAA_FLAG_SKIP_MULTICAST = 0x0004;
		public static int GAA_FLAG_SKIP_DNS_SERVER = 0x0008;
		public static int GAA_FLAG_INCLUDE_PREFIX = 0x0010;
		public static int GAA_FLAG_SKIP_FRIENDLY_NAME = 0x0020;
		public static int GAA_FLAG_INCLUDE_WINS_INFO = 0x0040;
		public static int GAA_FLAG_INCLUDE_GATEWAYS = 0x0080;
		public static int GAA_FLAG_INCLUDE_ALL_INTERFACES = 0x0100;
		public static int GAA_FLAG_INCLUDE_ALL_COMPARTMENTS = 0x0200;
		public static int GAA_FLAG_INCLUDE_TUNNEL_BINDINGORDER = 0x0400;

		@Structure.FieldOrder({"sin_family", "sin_port", "sin_addr", "sin_zero"})
		public static class sockaddr_in extends Structure {
			public sockaddr_in(Pointer p) {
				super(p);
				read();
			}

			public short sin_family;
			public short sin_port;
			public byte[] sin_addr = new byte[4];
			public byte[] sin_zero = new byte[8];
		}

		@Structure.FieldOrder({"sin6_family", "sin6_port", "sin6_flowinfo", "sin6_addr", "sin6_scope_id"})
		public static class sockaddr_in6 extends Structure {
			public sockaddr_in6(Pointer p) {
				super(p);
				read();
			}

			public short sin6_family;
			public short sin6_port;
			public int sin6_flowinfo;
			public byte[] sin6_addr = new byte[16];
			public int sin6_scope_id;
		}

		@Structure.FieldOrder({"lpSockaddr", "iSockaddrLength"})
		public static class SOCKET_ADDRESS extends Structure {
			public Pointer lpSockaddr;
			public int iSockaddrLength;

			public InetAddress toAddress() throws UnknownHostException {
				switch (lpSockaddr.getShort(0)) {
					case AF_INET:
						return InetAddress.getByAddress(new sockaddr_in(lpSockaddr).sin_addr);
					case AF_INET6:
						return InetAddress.getByAddress(new sockaddr_in6(lpSockaddr).sin6_addr);
				}

				return null;
			}
		}

		@Structure.FieldOrder({"Length", "IfIndex", "Next", "Address",
			"PrefixOrigin", "SuffixOrigin", "DadState", "ValidLifetime",
			"PreferredLifetime", "LeaseLifetime", "OnLinkPrefixLength"
		})
		public static class IP_ADAPTER_UNICAST_ADDRESS_LH extends Structure {
			public static class ByReference extends IP_ADAPTER_UNICAST_ADDRESS_LH implements Structure.ByReference {
			}

			public int Length;
			public int IfIndex;

			public IP_ADAPTER_UNICAST_ADDRESS_LH.ByReference Next;
			public SOCKET_ADDRESS Address;
			public int PrefixOrigin;
			public int SuffixOrigin;
			public int DadState;
			public int ValidLifetime;
			public int PreferredLifetime;
			public int LeaseLifetime;
			public byte OnLinkPrefixLength;
		}

		@Structure.FieldOrder({"Length", "Reserved", "Next", "Address"})
		public static class IP_ADAPTER_DNS_SERVER_ADDRESS_XP extends Structure {
			public static class ByReference extends IP_ADAPTER_DNS_SERVER_ADDRESS_XP implements Structure.ByReference {
			}

			public int Length;
			public int Reserved;
			public IP_ADAPTER_DNS_SERVER_ADDRESS_XP.ByReference Next;
			public SOCKET_ADDRESS Address;
		}

		@Structure.FieldOrder({"Length", "Reserved", "Next", "Address"})
		public static class IP_ADAPTER_ANYCAST_ADDRESS_XP extends Structure {
			public static class ByReference extends IP_ADAPTER_ANYCAST_ADDRESS_XP implements Structure.ByReference {
			}

			public int Length;
			public int Reserved;
			public IP_ADAPTER_DNS_SERVER_ADDRESS_XP.ByReference Next;
			public SOCKET_ADDRESS Address;
		}

		@Structure.FieldOrder({"Length", "Reserved", "Next", "Address"})
		public static class IP_ADAPTER_MULTICAST_ADDRESS_XP extends Structure {
			public static class ByReference extends IP_ADAPTER_MULTICAST_ADDRESS_XP implements Structure.ByReference {
			}

			public int Length;
			public int Reserved;
			public IP_ADAPTER_DNS_SERVER_ADDRESS_XP.ByReference Next;
			public SOCKET_ADDRESS Address;
		}

		@Structure.FieldOrder({"Next", "_String"})
		public static class IP_ADAPTER_DNS_SUFFIX extends Structure {
			public static class ByReference extends IP_ADAPTER_DNS_SUFFIX implements Structure.ByReference {
			}

			public IP_ADAPTER_DNS_SUFFIX.ByReference Next;
			public char[] _String = new char[256];
		}

		@Structure.FieldOrder({"Length", "IfIndex", "Next", "AdapterName", "FirstUnicastAddress", "FirstAnycastAddress", "FirstMulticastAddress",
			"FirstDnsServerAddress", "DnsSuffix", "Description", "FriendlyName", "PhysicalAddress", "PhysicalAddressLength", "Flags",
			"Mtu", "IfType", "OperStatus", "Ipv6IfIndex", "ZoneIndices", "FirstPrefix", "TransmitLinkSpeed", "ReceiveLinkSpeed",
			"FirstWinsServerAddress", "FirstGatewayAddress", "Ipv4Metric", "Ipv6Metric", "Luid", "Dhcpv4Server", "CompartmentId",
			"NetworkGuid", "ConnectionType", "TunnelType", "Dhcpv6Server", "Dhcpv6ClientDuid", "Dhcpv6ClientDuidLength", "Dhcpv6Iaid",
			"FirstDnsSuffix",
		})
		public static class IP_ADAPTER_ADDRESSES_LH extends Structure {
			public static class ByReference extends IP_ADAPTER_ADDRESSES_LH implements Structure.ByReference {
			}

			public IP_ADAPTER_ADDRESSES_LH(Pointer p) {
				super(p);
				read();
			}

			public IP_ADAPTER_ADDRESSES_LH() {
			}

			public int Length;
			public int IfIndex;

			public IP_ADAPTER_ADDRESSES_LH.ByReference Next;
			public String AdapterName;
			public IP_ADAPTER_UNICAST_ADDRESS_LH.ByReference FirstUnicastAddress;
			public IP_ADAPTER_ANYCAST_ADDRESS_XP.ByReference FirstAnycastAddress;
			public IP_ADAPTER_MULTICAST_ADDRESS_XP.ByReference FirstMulticastAddress;
			public IP_ADAPTER_DNS_SERVER_ADDRESS_XP.ByReference FirstDnsServerAddress;
			public WString DnsSuffix;
			public WString Description;
			public WString FriendlyName;
			public byte[] PhysicalAddress = new byte[8];
			public int PhysicalAddressLength;
			public int Flags;
			public int Mtu;
			public int IfType;
			public int OperStatus;
			public int Ipv6IfIndex;
			public int[] ZoneIndices = new int[16];
			public Pointer FirstPrefix;
			public long TransmitLinkSpeed;
			public long ReceiveLinkSpeed;
			public Pointer FirstWinsServerAddress;
			public Pointer FirstGatewayAddress;
			public int Ipv4Metric;
			public int Ipv6Metric;
			public Pointer Luid;
			public Pointer Dhcpv4Server;
			public int CompartmentId;
			public Guid.GUID NetworkGuid;
			public int ConnectionType;
			public int TunnelType;
			public Pointer Dhcpv6Server;
			public byte[] Dhcpv6ClientDuid = new byte[130];
			public int Dhcpv6ClientDuidLength;
			public int Dhcpv6Iaid;
			public IP_ADAPTER_DNS_SUFFIX.ByReference FirstDnsSuffix;
		}

		int GetAdaptersAddresses(int family, int flags, Pointer reserved, Pointer adapterAddresses, IntByReference sizePointer);
	}
}
