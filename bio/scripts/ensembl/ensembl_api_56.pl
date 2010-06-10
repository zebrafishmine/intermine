#!/usr/bin/perl
# translates data from ensembl database to intermine items XML file

BEGIN {
    unshift (@INC, ($0 =~ m:(.*)/.*:)[0] . '/../../../intermine/perl/lib');    
}

use strict;
use warnings;
use Switch;

use Getopt::Std;

use XML::Writer;
use InterMine::Item;
use InterMine::ItemFactory;
use InterMine::Model;
use Bio::EnsEMBL::DBSQL::DBAdaptor;
use InterMine::Util qw(parse_properties_file);
use IO qw(Handle File);
use Digest::MD5 qw(md5);

my $script_dir = ($0 =~ m:(.*)/.*:)[0];

our ($opt_r, $opt_l);
getopt('rl');

if (@ARGV != 3) {
  die "usage: [-r release] [-l logfile] mine_name taxon_id data_destination\n"
     ."eg.    [-r preview] [-l out.log] flymine   9606     /shared/data/ensembl/current\n";  
}

# vars from the command line
my ($mine_name, $taxon_ids, $data_destination) = @ARGV;

# just for logging purposes
my $start_time = time();

my $logfile = $opt_l;
my $loghandle;
if ($logfile) {
    open $loghandle, '>>', $logfile or die $!;
}
else {
    $loghandle = new IO::Handle;
    $loghandle->fdopen( \*STDERR,  '>>' ) or die $!;
}
my @times = localtime($start_time);
print $loghandle 
    '[',
    join('-', ($times[5] + 1900), ($times[4]+1), $times[3]),
    " $times[2]:$times[1]] Running $0\n";

# Set-up the intermine item-creating apparatus
my $release = ($opt_r)? '.' . $opt_r : '';
my $model_file = $script_dir . "/../../../$mine_name/dbmodel/build/main/genomic_model.xml";
my $properties_file = "$ENV{HOME}/.intermine/$mine_name.properties" . $release;

for ($model_file, $properties_file) {
    die "Cannot read $_" unless -r;
}

my $model = new InterMine::Model(file => $model_file);
my $properties = parse_properties_file($properties_file);

my $item_factory = new InterMine::ItemFactory(model => $model);

# init vars which need to be globally accessible
#my (@items,
my (%item_store);
my %organisms = parse_orgs($taxon_ids);
my $datasource = 'Ensembl';
my ($org_item, $dataset_item, $datasource_item, %genesncbis, %ncbisgenes); # of these, only the two hashes should grow,
                                                         # getting to about 500kb for every 200mb of xml


# config file to tell us which chromosomes to bother with for which organisms
# the default is _all_ chromosomes
my $config_file = $script_dir.'/../../sources/ensembl/resources/ensembl_config.properties';
%organisms = parse_config($config_file, %organisms);

foreach my $taxon_id (keys %organisms) {

    ############################################
    # Set up the xml writing apparatus
    my $output = new IO::File(">$data_destination/$taxon_id.xml") or die $!;
    my $writer = new XML::Writer(DATA_MODE => 1, DATA_INDENT => 3, OUTPUT => $output);
    $writer->startTag('items');
    ############################################
    
    ############################################
    # Set info for datasource and write it out
    $datasource_item = make_item("DataSource");
    $datasource_item->set('name', $datasource);
    $datasource_item->{':uniq'} = $datasource;
    writeout($datasource_item, $writer);
    ############################################

    ############################################
    # Set info for organism and write it out
    $org_item = make_item("Organism"); 
    $org_item->set(taxonId => $taxon_id);
    $org_item->{':uniq'} = $taxon_id;
    writeout($org_item, $writer);
    ############################################

    ############################################
    # Set info for dataset and write it out
    $dataset_item = make_item("DataSet");
    $dataset_item->set(name  => "$datasource data set for taxon id: $taxon_id");
    $dataset_item->{':uniq'} = $datasource . $taxon_id;
    writeout($dataset_item, $writer);
    ############################################

    my %chromosomes = ();
    my $chromosomes_string = $organisms{$taxon_id};

    if ($chromosomes_string) {
        %chromosomes = parse_chromosomes($chromosomes_string);
    }

    my $host    = $properties->{"db.ensembl.${taxon_id}.core.datasource.serverName"};
    my $dbname  = $properties->{"db.ensembl.${taxon_id}.core.datasource.databaseName"};
    my $user    = $properties->{"db.ensembl.${taxon_id}.core.datasource.user"};
    my $pass    = $properties->{"db.ensembl.${taxon_id}.core.datasource.password"};
    my $species = $properties->{"db.ensembl.${taxon_id}.core.datasource.species"};

    my $dbCore = Bio::EnsEMBL::DBSQL::DBAdaptor->new(
	 -host    => $host,
	 -user    => $user,
	 -pass    => $pass,
         -dbname  => $dbname,
	 -species => $species,
	 -group   => 'core', # This obviously needs to be changed if variations will be available
	);

    my $slice_adaptor = $dbCore->get_sliceAdaptor();
    my $slices = $slice_adaptor->fetch_all('toplevel');
    
    my %seen_chromosomes;
    while (my $chromosome = shift @{$slices}) { # Ugly, but supposedly more memory-efficient way of doing things
                                          # cf: http://ncbi36.ensembl.org/info/docs/api/core/core_tutorial.html
	my $chromosome_name = $chromosome->seq_region_name();
	next if ($chromosomes_string && !exists $chromosomes{$chromosome_name});# not on a chromosome of interest
	next if $seen_chromosomes{$chromosome_name}++;

	warn "Processing chromosome $chromosome_name ", time - $start_time, "\n";

	############################################
	# Set info for chromosome and write it out
        my ($chromosome_item, $syn) = make_chromosome($chromosome_name);
	writeout($chromosome_item, $writer);
	writeout($syn, $writer);
	############################################

        my $genes = $chromosome->get_all_Genes; # load genes lazily - then they can be dumped later
	                                                        
        while (my $gene = shift @$genes) {
	    
	    ############################################
	    # Set info for gene and write it out
            my $gene_item = make_item("Gene");
            my $gene_type = $gene->biotype();
            
            $gene_item->set(featureType => $gene_type);
	    #removed from model
#           $gene_item->set(curated     => ($gene->status eq 'KNOWN')? 'true' : 'false'); 
            
            my $location = parse_feature($gene, $gene_item, $chromosome_item);  # fills in the info for gene_item
	    add_xrefs($gene, $gene_item) if ($taxon_id == 9606); # add cross references 
                                                                 # when run for humans
	    writeout($gene_item, $writer);
	    writeout($location, $writer);
	    
            my $syn = make_synonym($gene_item, "identifier", $gene->stable_id());
	    writeout($syn, $writer);
            ############################################	    
            
	    my $transcripts = $gene->get_all_Transcripts();
            while (my $transcript = shift @$transcripts) {
                
		############################################
		# Set info for transcript and write it out
                my $transcript_item;
                if ($gene_type eq "protein_coding") {
                    $transcript_item = make_item("MRNA");
                } else {
                    $transcript_item = make_item("Transcript");
                }

		my $location = parse_feature($transcript, $transcript_item, $chromosome_item);
		  writeout($location, $writer);
                $transcript_item->set(gene     => $gene_item);
		my $seq = make_seq($transcript->seq->seq);
		  writeout($seq, $writer);
                $transcript_item->set(sequence => $seq);
                  writeout($transcript_item, $writer);
		  # TODO are these transcripts going to be unique?
                my $syn = make_synonym($transcript_item, "identifier", $transcript->stable_id());
		  writeout($syn, $writer);
		############################################

		if ($transcript->biotype() eq "protein_coding") {

                    my $translation = $transcript->translate();
                    if (!defined $translation) {
                        warn "no translation for gene: " . $gene->stable_id() . "\n";
                        next;
                    }
		    
		    ############################################	
		    # Set protein information and write out
                    my $protein_seq  = $translation->seq();
                    my ($protein_item, $prot_seq_item) = make_protein($protein_seq);
                
                    $protein_item->set(genes       => [$gene_item]);
                    $protein_item->set(transcripts => [$transcript_item]);

		    writeout($protein_item, $writer);
		    writeout($prot_seq_item, $writer);
 		    ############################################

		    ############################################
		    # Set CDS information and write out
                    my $cds_item              = make_item("CDS");
                    my $cds_primaryIdentifier = $transcript->stable_id() . "_CDS";
		    my $sequence              = make_seq($transcript->translateable_seq());
		    writeout($sequence, $writer);
		    
                    $cds_item->set(primaryIdentifier => $cds_primaryIdentifier);                    
                    $cds_item->set(sequence          => $sequence);
                    $cds_item->set(MRNA              => $transcript_item);
                    $cds_item->set(protein           => $protein_item);
		    $cds_item->{':uniq'} = $cds_primaryIdentifier;

		    writeout($cds_item, $writer);
		    ############################################
                }

		my $exons = $transcript->get_all_Exons();
		while (my $exon = shift @$exons) {
		    
		    ############################################
		    # Set exon information and write out
                    my $primary_identifier = $exon->stable_id();
                    my ($exon_item, $syn)  = make_exon($primary_identifier);
		    my $seq_item           = make_seq($exon->seq->seq);

                    my $location = parse_feature($exon, $exon_item, $chromosome_item);     
                    $exon_item->set(transcripts => [$transcript_item]);
                    $exon_item->set(gene        => $gene_item);                    
                    $exon_item->set(sequence    => $seq);

		    writeout($exon_item, $writer);		
		    writeout($syn, $writer);
		    writeout($seq_item, $writer);
		    writeout($location, $writer);
		    ############################################
                }
            }
        }    
    }

    $writer->endTag('items');
    $writer->end();
    $output->close();
    my $end_time = time();
    my $action_time = $end_time - $start_time;
    print "creating the XML file for $taxon_id took $action_time seconds.\n";

    undef $writer;

   my $i;
    for my $hash (\%genesncbis, \%ncbisgenes) {
	my $thing = ($i++ > 0)? 'ncbi' : 'gene';
	for (keys %$hash) {
	    my %seen = ();
	    my @uniqs = grep { ! $seen{$_} ++ } @{$hash->{$_}};
	    if (@uniqs > 1) {
		print $loghandle scalar(@uniqs), 
		  " matches for $thing $_: ",
		  join(' ', @uniqs),
		  "\n";
	    }
	}
   }

}

my $status = check_output();
exit $status;

BEGIN {
    my $has_been_written = {}; # state variables written as closures for compatibility with v5.8
    my $total_written;
    my $total_made;
    sub writeout {
	my $item       = shift;
	my $writer     = shift;
	my $uniq       = $item->{':uniq'};
	my $implements = $item->{':implements'};
	my $this_id    = $item->{'id'};
	my $already_written = $has_been_written->{$implements}->{$uniq};

	if ($already_written) {
	    &announce_duplicate($this_id, $implements, $uniq, $already_written)
		unless ($already_written == $this_id); 
                # ie. if it isn't _this_ item which we wrote
	    return;
	}
	else {
	    $item->as_xml($writer);
	    $has_been_written->{$implements}->{$uniq} = $this_id;
	    $total_written++;
	}
	return;
    }
    
    sub make_item {
	my $implements = shift;
	my $item = $item_factory->make_item(implements => $implements);
	if ($item->valid_field('organism')) {
	    $item->set('organism', $org_item);
	}
	if ($item->valid_field('dataSets') && $implements ne 'DataSource') {
	    $item->set('dataSets', [$dataset_item]);
    }
	if ($item->valid_field('dataSource')) {
	    $item->set('dataSource', $datasource_item);
	}
	$total_made++;
	return $item;
    }

    sub check_output {
	if ($total_made != $total_written) {
	    warn "We made $total_made items but wrote out $total_written!\n", 
                 "Something went wrong: please check your logs\n";
	}
	return $total_made - $total_written;
    }
}


sub announce_duplicate {
    my $complaint = sprintf("item %s (%s %s) duplicates item %s\n", @_);
    print $loghandle $complaint;
    return;
}


# parses the feature returned from ensembl and 
# assigns the classes to the intermine item
# used for genes, transcripts, and exons.  
sub parse_feature {
    my ($feature, $item, $chromosome) = @_;    

    $item->set(primaryIdentifier  => $feature->stable_id());   
    $item->set(chromosome         => $chromosome);

    my $location                  = make_location($feature, $item, $chromosome);
    $item->set(chromosomeLocation => $location);

    $item->{':uniq'} = $feature->stable_id();

    return $location;
}

# adds cross references - Andrew Vallejos
# currently only adds NCBI Gene Number
sub add_xrefs {
    my ($feature, $item) = @_;
    my $gene_name        = $item->get('primaryIdentifier');

    foreach my $xref (@{$feature->get_all_DBLinks}) {
	#probably a much better way of doing this exists, but could be useful for loading all
	#known xRefs for a record
	if ($xref->dbname eq 'EntrezGene') {
	    my $ncbi_no = $xref->primary_id;
	    $item->set(ncbiGeneNumber => $ncbi_no);
	    push @{$genesncbis{$gene_name}}, $ncbi_no;
	    push @{$ncbisgenes{$ncbi_no}}, $gene_name;
	}
    }   
}

# read in the config file 
sub read_file { # Why is this not in the InterMine::Util module?
    my($filename) = shift;
    my @lines     = ();

    open(FILE, "< $filename") or die "Can't open $filename : $!";
    while(<FILE>) {
        s/#.*//;            # ignore comments by erasing them
        next if /^(\s)*$/;  # skip blank lines
        chomp;              # remove trailing newline characters
        push @lines, $_;    # push the data line onto the array
    }
    close FILE;
    return @lines;  
}

# parse the config file
sub parse_config {
    my ($file, %organisms) = @_;
    my @lines = read_file($file);
    foreach (@lines) {
        my $line = $_;
        my ($taxon_id, $config) = split('\.', $line);
        my ($label, $value)     = split('=', $config);
        if ($label eq 'chromosomes' && defined $organisms{$taxon_id}) {
            $organisms{$taxon_id} = $value;
        }
    }
    return %organisms;
}

sub make_location {
    my ($feature, $item, $chromosome) = @_;
    my $location;

    my $start        = $feature->start();
    my $end          = $feature->end();
    my $itemId       = $item->get('primaryIdentifier');
    my $chromosomeId = $chromosome->get('primaryIdentifier');
    my $key          = join('|', $start, $end, $itemId, $chromosomeId);
    
    unless ( exists $item_store{locations}{$key} ) {

	$location = make_item('Location');

	$location->set(start      => $start);
	$location->set(end        => $end);
	$location->set(strand     => $feature->strand());
	$location->set(feature    => $item);
	$location->set(locatedOn  => $chromosome);
   
	$location->{':uniq'}         = $key;
	$item_store{locations}{$key} = $location;
    }

    return $item_store{locations}{$key}; 
}

sub make_synonym {
    my ($subject, $type, $value) = @_;
    
    my $syn = make_item("Synonym");
    
    $syn->set(subject   => $subject);
    $syn->set(type      => $type);
    $syn->set(value     => $value);
    $syn->set(isPrimary => 'true');
    
    my $key = $subject . $type . $value;
    $syn->{':uniq'} = $key;
    
    return $syn;
}

sub make_chromosome {

    my $name = shift;
    
    unless (exists $item_store{chromosomes}{real}{$name}) {

	my $chromosome_item                     = make_item('Chromosome');
	$chromosome_item->set(primaryIdentifier => $name);
	$chromosome_item->{':uniq'}             = $name;

	$item_store{chromosomes}{real}{$name} = $chromosome_item;
	$item_store{chromosomes}{syn}{$name} 
	         = make_synonym($chromosome_item, 'identifier', $name);
    }
    
    return $item_store{chromosomes}{real}{$name}, 
           $item_store{chromosomes}{syn}{$name};
}   

sub make_protein {

    my $seq         = shift;
    my $md5checksum = encodeSeq($seq);
    
    unless ( exists $item_store{proteins}{$md5checksum} ) {

	my $protein_item = make_item('Protein');
	my $seq_item     = make_seq($seq);

	$protein_item->set(sequence    => $seq_item);
	$protein_item->set(md5checksum => $md5checksum);
	$protein_item->{':uniq'}       =  $md5checksum;

	$item_store{proteins}{$md5checksum}     = $protein_item;
	$item_store{protein_seqs}{$md5checksum} = $seq_item;
    }

    return $item_store{proteins}{$md5checksum},
           $item_store{protein_seqs}{$md5checksum};
}

sub make_exon {
    my $primary_id = shift;
    
    unless (exists $item_store{exons}{real}{$primary_id}) {
	my $exon_item = make_item("Exon"); 

	$exon_item->set(primaryIdentifier    => $primary_id);
	$exon_item->{':uniq'}                 = $primary_id;

	$item_store{exons}{real}{$primary_id} = $exon_item;
	$item_store{exons}{syn}               = make_synonym($exon_item, "identifier", $primary_id);
    }
    
    return $item_store{exons}{real}{$primary_id},
           $item_store{exons}{syn};
}


sub make_seq {

    my $seq = shift;
    my $md5checksum = encodeSeq($seq);
    
    unless (exists $item_store{seqs}{$md5checksum}) {
	my  $seq_item = make_item('Sequence');
	$seq_item->set(residues    => $seq);
	$seq_item->set(length      => length($seq));
	$seq_item->set(md5checksum => $md5checksum);
    
	$seq_item->{':uniq'} = $md5checksum;
	$item_store{seqs}{$md5checksum} = $seq_item;
    }
    return $item_store{seqs}{$md5checksum}; #$seq_item;
}   

sub encodeSeq {

    my $seq = shift;

    my $ctx = Digest::MD5->new;
    $ctx->add($seq);
    return $ctx->hexdigest;
    
}

# user can specify which chromosomes to load  
# eg 1-21,X,Y
sub parse_chromosomes {
    my ($chromosome_string) = shift;
  
    my @bits = split(",", $chromosome_string);
    my %chromosomes;
 
    foreach (@bits) {
        my $bit = $_;

        # list may be a range
        if ($bit =~ "-") {
            my @range = split("-", $bit);
            my $min = $range[0];
            my $max = $range[1];
	    die "Could not parse chromosome string: $chromosome_string\n" if ($min > $max);

            for my $i ($min .. $max) {
                $chromosomes{$i} = undef;
            }
        } else {
            $chromosomes{$bit} = undef;
        }
    }
    return %chromosomes;
}


sub parse_orgs { # returns a hash with the keys set to the comma-delimited set of taxon ids
                 # and values all set to ''
    my $taxon_ids = shift;
    my %orgs;    
    for (split(',', $taxon_ids)) {
        $orgs{$_} = "";
    }
    return %orgs;
}

exit 0;
