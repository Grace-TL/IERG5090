package org.lal.app;
import java.io.IOException;
import com.google.common.hash.HashFunction;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.hash.Hasher;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class LCount {

    protected CountMin lc;

    protected int depth;

    protected int width;

    protected int lwidth;

    protected int count;

    protected CountMin counts[][];

    protected int[] min_index;

    protected int[] num_zero;

    protected int[] seed;

    protected int lgn = 104; 

    LCount(int width, int depth, int lc_len){
	MyUtil myutil = new MyUtil();
	this.width = (int)(myutil.getPrime(width));	
        this.depth = depth;
	this.lwidth = lc_len;
	this.counts = new CountMin[depth][width];
	this.min_index = new int[depth];
	this.num_zero = new int[depth];
	this.lc = new CountMin((int)myutil.getPrime(lc_len),4);
	Arrays.fill(min_index,  width);
	Arrays.fill(num_zero,  width);
	seed = new int[depth];
	
	for(int i = 1; i < seed.length; i++)
	    seed[i] = ThreadLocalRandom.current().nextInt();
    }

    public synchronized void update(Header header, int val){
	System.out.println("Now is LCM-update!");
	Header sipheader = new Header(header.getSrcIp(),0,(short)0,(short)0,(byte)0);
	this.lc.update(header, 1);
	for(int i = 0; i < this.depth; i++){
	    int bucket = (int)(hash(sipheader, seed[i])%this.width);
	    if(this.counts[i][bucket] == null){
		CountMin nlc = new CountMin((int)new MyUtil().getPrime(this.lwidth), 4);
		this.counts[i][bucket] = nlc;
	        this.num_zero[i]--;
	    }
   	    if(this.min_index[i] > bucket)
		min_index[i] = bucket;
            Header nheader = new Header(0,header.getDstIp(),(short)0,(short)0,(byte)0);
	    counts[i][bucket].update(nheader, val);
	}
    }

    public int pointEst(Header header){
	Header sipheader = new Header(header.getSrcIp(),0,(short)0,(short)0,(byte)0);
	int bucket = (int)(hash(sipheader, seed[0])%this.width);
	int min = this.counts[0][bucket].linearCount();
	for(int i = 1; i < this.depth; i++){
	    int bucketi  = (int)(hash(sipheader,seed[i])%this.width);
	    if(min > counts[i][bucketi].linearCount())
	        min = this.counts[i][bucketi].linearCount();
	}
	return min;
    }

    public int linearCount(){
	int ret;
	int[] ans = new int[this.depth];
	for(int i = 0; i < this.depth; i ++){
	    if(this.num_zero[i] == 0)
		ans[i] = (int)(Math.log(this.depth/2)*this.width);
	    else{
		double p = 1.0*this.width/num_zero[i];
		ans[i] = (int)(Math.log(p) * this.width);
	    }
	}
	Arrays.sort(ans);
	if(ans.length % 2 == 0)
	    ret = (ans[ans.length/2]+ans[ans.length/2+1])/2;
	else
	    ret = ans[ans.length/2];
	return ret;
    }
 
    private long hash(Header header, int seed){
	HashFunction hf = Hashing.murmur3_128(seed);
	HashCode hc = hf.newHasher()
	    	.putInt(header.getSrcIp())
	    	.putInt(header.getDstIp())
	    	.putShort(header.getSrcPort())
	    	.putShort(header.getDstPort())
	    	.putByte(header.getProtocol())
	    	.hash();
	long value = hc.asLong();
	if(value < 0)
	    value = -value;
	return value;
    }

    public void clear(){
	lc.clear();
	for(CountMin[] row : this.counts)
	    for(CountMin rcm : row)
		rcm.clear();
	Arrays.fill(min_index,  width);
	Arrays.fill(num_zero,  width);



    }

    public long getCount(){
	this.count = this.lc.linearCount();
	return this.count;
    }	

    public int getWidth(){
	return this.width;
    }

    public int getDepth(){
	return this.depth;
    }
}
