package org.bouncycastle.tls;

import java.util.Vector;

import org.bouncycastle.util.Strings;

public final class ProtocolVersion
{
    public static final ProtocolVersion SSLv3 = new ProtocolVersion(0x0300, "SSL 3.0");
    public static final ProtocolVersion TLSv10 = new ProtocolVersion(0x0301, "TLS 1.0");
    public static final ProtocolVersion TLSv11 = new ProtocolVersion(0x0302, "TLS 1.1");
    public static final ProtocolVersion TLSv12 = new ProtocolVersion(0x0303, "TLS 1.2");
    public static final ProtocolVersion DTLSv10 = new ProtocolVersion(0xFEFF, "DTLS 1.0");
    public static final ProtocolVersion DTLSv12 = new ProtocolVersion(0xFEFD, "DTLS 1.2");

    public static boolean contains(ProtocolVersion[] versions, ProtocolVersion version)
    {
        if (versions != null && version != null)
        {
            for (int i = 0; i < versions.length; ++i)
            {
                if (version.equals(versions[i]))
                {
                    return true;
                }
            }
        }
        return false;
    }

    public static ProtocolVersion getEarliest(ProtocolVersion[] versions)
    {
        if (null == versions || versions.length < 1)
        {
            return null;
        }

        ProtocolVersion earliest = versions[0];
        int majorVersion = earliest.getMajorVersion();

        for (int i = 1; i < versions.length; ++i)
        {
            ProtocolVersion next = versions[i];
            if (next.getMajorVersion() != majorVersion)
            {
                throw new IllegalArgumentException("'versions' entries must all have the same major version");
            }
            if (earliest.isLaterVersionOf(next))
            {
                earliest = next;
            }
        }

        return earliest;
    }

    public static ProtocolVersion getLatest(ProtocolVersion[] versions)
    {
        if (null == versions || versions.length < 1)
        {
            return null;
        }

        ProtocolVersion latest = versions[0];
        int majorVersion = latest.getMajorVersion();

        for (int i = 1; i < versions.length; ++i)
        {
            ProtocolVersion next = versions[i];
            if (next.getMajorVersion() != majorVersion)
            {
                throw new IllegalArgumentException("'versions' entries must all have the same major version");
            }
            if (next.isLaterVersionOf(latest))
            {
                latest = next;
            }
        }

        return latest;
    }

    private int version;
    private String name;

    private ProtocolVersion(int v, String name)
    {
        this.version = v & 0xFFFF;
        this.name = name;
    }

    public ProtocolVersion[] downTo(ProtocolVersion min)
    {
        if (!min.isEqualOrEarlierVersionOf(this))
        {
            throw new IllegalArgumentException("'min' must be an equal or earlier version of this one");
        }

        Vector result = new Vector();
        result.addElement(this);

        ProtocolVersion current = this;
        while (!current.equals(min))
        {
            current = current.getPreviousVersion();
            result.addElement(current);
        }

        ProtocolVersion[] versions = new ProtocolVersion[result.size()];
        for (int i = 0; i < result.size(); ++i)
        {
            versions[i] = (ProtocolVersion)result.elementAt(i);
        }
        return versions;
    }

    public int getFullVersion()
    {
        return version;
    }

    public int getMajorVersion()
    {
        return version >> 8;
    }

    public int getMinorVersion()
    {
        return version & 0xFF;
    }

    public boolean isDTLS()
    {
        return getMajorVersion() == 0xFE;
    }

    public boolean isTLS()
    {
        return getMajorVersion() == 0x03;
    }

    public ProtocolVersion getEquivalentTLSVersion()
    {
        switch (getMajorVersion())
        {
        case 0x03:  return this;
        case 0xFE:
            switch(getMinorVersion())
            {
            case 0xFF:  return TLSv11;
            case 0xFD:  return TLSv12;
            default:    return null;
            }
        default:    return null;
        }
    }

    public ProtocolVersion getPreviousVersion()
    {
        if (isDTLS())
        {
            switch(getMinorVersion())
            {
            case 0xFF: return null;
            case 0xFD: return DTLSv10;
            default  : return get(getMajorVersion(), getMinorVersion() + 1);
            }
        }
        else
        {
            switch (getMinorVersion())
            {
            case 0x00: return null;
            default  : return get(getMajorVersion(), getMinorVersion() - 1);
            }
        }
    }

    public boolean isEqualOrEarlierVersionOf(ProtocolVersion version)
    {
        if (getMajorVersion() != version.getMajorVersion())
        {
            return false;
        }
        int diffMinorVersion = version.getMinorVersion() - getMinorVersion();
        return isDTLS() ? diffMinorVersion <= 0 : diffMinorVersion >= 0;
    }

    public boolean isLaterVersionOf(ProtocolVersion version)
    {
        if (getMajorVersion() != version.getMajorVersion())
        {
            return false;
        }
        int diffMinorVersion = version.getMinorVersion() - getMinorVersion();
        return isDTLS() ? diffMinorVersion > 0 : diffMinorVersion < 0;
    }

    public boolean equals(Object other)
    {
        return this == other || (other instanceof ProtocolVersion && equals((ProtocolVersion)other));
    }

    public boolean equals(ProtocolVersion other)
    {
        return other != null && this.version == other.version;
    }

    public int hashCode()
    {
        return version;
    }

    public static ProtocolVersion get(int major, int minor)
    {
        switch (major)
        {
        case 0x03:
        {
            switch (minor)
            {
            case 0x00:
                return SSLv3;
            case 0x01:
                return TLSv10;
            case 0x02:
                return TLSv11;
            case 0x03:
                return TLSv12;
            }
            return getUnknownVersion(major, minor, "TLS");
        }
        case 0xFE:
        {
            switch (minor)
            {
            case 0xFF:
                return DTLSv10;
            case 0xFE:
                throw new IllegalArgumentException("{0xFE, 0xFE} is a reserved protocol version");
            case 0xFD:
                return DTLSv12;
            }
            return getUnknownVersion(major, minor, "DTLS");
        }
        default:
        {
            return getUnknownVersion(major, minor, "UNKNOWN");
        }
        }
    }

    public ProtocolVersion[] only()
    {
        return new ProtocolVersion[]{ this };
    }

    public String toString()
    {
        return name;
    }

    private static void checkUint8(int versionOctet)
    {
        if (!TlsUtils.isValidUint8(versionOctet))
        {
            throw new IllegalArgumentException("'versionOctet' is not a valid octet");
        }
    }

    private static ProtocolVersion getUnknownVersion(int major, int minor, String prefix)
    {
        checkUint8(major);
        checkUint8(minor);

        int v = (major << 8) | minor;
        String hex = Strings.toUpperCase(Integer.toHexString(0x10000 | v).substring(1));
        return new ProtocolVersion(v, prefix + " 0x" + hex);
    }
}
