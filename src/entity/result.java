package entity;

import java.util.ArrayList;
import java.util.List;
import java.util.List;

public class result {
    public List<List<Double>> pfs;
    public List<Job> jobs;
    public List<ETS> etss;
   public result(List<List<Double>> pfs,List<Job> jobs,List<ETS>etss){
       this.pfs=new ArrayList<List<Double>>(pfs);
       this.jobs=new ArrayList<Job>(jobs);
       this.etss=new ArrayList<ETS>(etss);
   }
    public result(List<List<Double>> pfs,List<Job> jobs){
        this.pfs=new ArrayList<List<Double>>(pfs);
        this.jobs=new ArrayList<Job>(jobs);
        this.etss=null;
    }

}
