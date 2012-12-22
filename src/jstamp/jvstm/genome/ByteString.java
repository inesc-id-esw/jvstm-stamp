package jstamp.jvstm.genome;


public class ByteString {
    // ALL shared
    private final byte[] value;
    private final int count;
    private final int offset;
    private final int cachedHashcode;

    private ByteString() {
	this.value = null;
	this.count = 0;
	this.offset = 0;
	this.cachedHashcode = 0;
    }

    public ByteString(byte str[]) {
	this.value = str;
	this.count = str.length;
	this.offset = 0;
	int hash = 0;
	int off = offset;
	int myCount = this.count;
	byte[] myValue = this.value;
	for(int index = 0; index < myCount; index++) {
	    byte c = myValue[index+off];
	    hash = c + (hash << 6) + (hash << 16) - hash;
	}
	if (hash<0)
	    hash=-hash;
	this.cachedHashcode = hash;
    }
    
    public ByteString(byte str[], int count, int offset) {
	this.value = str;
	this.count = count;
	this.offset = offset;
	int hash = 0;
	int off = offset;
	int myCount = this.count;
	byte[] myValue = this.value;
	for(int index = 0; index < myCount; index++) {
	    byte c = myValue[index+off];
	    hash = c + (hash << 6) + (hash << 16) - hash;
	}
	if (hash<0)
	    hash=-hash;
	this.cachedHashcode = hash;
    }

    public int compareTo(ByteString s) {
	int myCount = count;
	int sCount = s.count;
	int smallerlength = myCount < sCount ? myCount : sCount;

	int off= offset;
	int soff= s.offset;
	byte[] myValue = this.value;
	byte[] sValue = s.value;
	for( int i = 0; i < smallerlength; i++) {
	    int valDiff = myValue[i+off] - sValue[i+soff];
	    if( valDiff != 0 ) {
		return valDiff;
	    }
	}
	return myCount - sCount;
    }
    
    public String toString() {
	String s = "count: " + count + " offset: " + offset;
	byte[] v = value;
	s += " [";
	for (byte b : v) {
	    s += ", " + b; 
	}
	s += " ]";
	return s;
    }

    public boolean endsWith(ByteString suffix) {
	int myCount = this.count;
	int suffixCount = suffix.count;
	return regionMatches(myCount - suffixCount, suffix, 0, suffixCount);
    }

    public ByteString substring(int beginIndex) {
	return substring(beginIndex, this.count);
    }

    public ByteString subString(int beginIndex, int endIndex) {
	return substring(beginIndex, endIndex);
    }

    public ByteString substring(int beginIndex, int endIndex) {
	int myCount = this.count;
	if (beginIndex > myCount || endIndex > myCount || beginIndex > endIndex) {
	    // FIXME
	    System.out.println("Index error: "+beginIndex+" "+endIndex+" "+count+"\n"+this);
	    System.exit(-1);
	}
	return new ByteString(this.value, endIndex - beginIndex, this.offset + beginIndex);
    }

    public ByteString subString(int beginIndex) {
	return this.subString(beginIndex, this.count);
    }

    public int lastindexOf(int ch) {
	return this.lastindexOf(ch, count - 1);
    }

    public ByteString concat(ByteString str) {
	int myCount = this.count;
	int stroffset = str.offset;
	int strCount = str.count;
	byte charstr[] = new byte[myCount + strCount];
	ByteString newstr = new ByteString(charstr, charstr.length, 0);
	byte[] myValue = value;
	byte[] strValue = str.value;
	int myOffset = offset;
	for(int i = 0; i < myCount; i++) {
	    charstr[i]= myValue[i + myOffset];
	}
	for(int i = 0; i < strCount; i++) {
	    charstr[i + myCount] = strValue[stroffset];
	}
	return newstr;
    }

    public int lastindexOf(int ch, int fromIndex) {
	int off = offset;
	byte[] myValue = this.value;
	for(int i = fromIndex; i > 0; i--)
	    if (myValue[i + off] == ch)
		return i;
	return -1;
    }

    public int indexOf(int ch) {
	return this.indexOf(ch, 0);
    }

    public int indexOf(int ch, int fromIndex) {
	int off = offset;
	int myCount = count;
	byte[] myValue = this.value;
	for(int i = fromIndex; i < myCount; i++)
	    if (myValue[i + off] == ch)
		return i;
	return -1;
    }

    public int indexOf(ByteString str) {
	return this.indexOf(str, 0);
    }

    public int indexOf(ByteString str, int fromIndex) {
	if (fromIndex < 0)
	    fromIndex = 0;
	int myCount = count;
	int sCount = str.count;
	for(int i = fromIndex; i <= (myCount - sCount); i++)
	    if (regionMatches(i, str, 0, sCount))
		return i;
	return -1;
    }

    public int lastIndexOf(ByteString str, int fromIndex) {
	int sCount = str.count;
	int k = count - sCount;
	if (k>fromIndex)
	    k=fromIndex;
	for(; k>=0; k--) {
	    if (regionMatches(k, str, 0, sCount))
		return k;
	}
	return -1;
    }

    public int lastIndexOf(ByteString str) {
	return lastIndexOf(str, count - str.count);
    }

    public boolean startsWith(ByteString str) {
	return regionMatches(0, str, 0, str.count);
    }

    public boolean startsWith(ByteString str, int toffset) {
	return regionMatches(toffset, str, 0, str.count);
    }

    public boolean regionMatches(int toffset, ByteString other, int ooffset, int len) {
	int myCount = this.count;
	int otherCount = other.count;
	byte[] otherValue = other.value;
	byte[] myValue = this.value;
	int myOffset = this.offset;
	int otherOffset = other.offset;
	
	if (toffset < 0 || ooffset < 0 || (toffset + len) > myCount || (ooffset + len) > otherCount)
	    return false;
	
	for(int i=0; i<len; i++)
	    if (otherValue[i + otherOffset + ooffset] != myValue[i + myOffset + toffset])
		return false;
	return true;
    }

    public byte[] getBytes() {
	int myCount = this.count;
	byte str[] = new byte[myCount];
	byte[] myValue = this.value;
	int myOffset = this.offset;
	for(int i = 0; i < myCount; i++)
	    str[i]= (byte)myValue[i + myOffset];
	return str;
    }

    public int length() {
	return count;
    }

    public byte byteAt(int i) {
	return value[i + offset];
    }

    public int hashCode() {
	return this.cachedHashcode;
    }

    public boolean equals(Object o) {
	if (o.getClass()!=getClass())
	    return false;
	ByteString s=(ByteString)o;
	if (s.count!=count)
	    return false;
	int myCount = this.count;
	byte[] sValue = s.value;
	byte[] myValue = this.value;
	int sOffset = s.offset;
	int myOffset = this.offset;
	for(int i=0; i < myCount; i++) {
	    if (sValue[i + sOffset] != myValue[i + myOffset])
		return false;
	}
	return true;
    }
}
