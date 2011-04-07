/**
 * 
 */
package com.manning.sbia.ch01.batch;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author acogoluegnes
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/import-products-job-context.xml","/test-context.xml"})
public class ImportProductsIntegrationTest {
	
	@Autowired
	private JobLauncher jobLauncher;
	
	@Autowired
	private Job job;
	
	private SimpleJdbcTemplate jdbcTemplate;
	
	@Autowired
	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}
	
	@Before
	public void setUp() throws Exception {
		jdbcTemplate.update("delete from product");
		jdbcTemplate.update(
			"insert into product (id,name,description,price) values(?,?,?,?)",
			"PR....214","Nokia 2610 Phone","",102.23	
		);
	}

	@Test public void importProducts() throws Exception {
		int initial = jdbcTemplate.queryForInt("select count(1) from product");
		
		jobLauncher.run(job, new JobParametersBuilder()
			.addString("inputResource", "classpath:/input/products.zip")
			.addString("targetDirectory", "./target/importproductsbatch/")
			.addString("targetFile","products.txt")
			.addLong("timestamp", System.currentTimeMillis())
			.toJobParameters()
		);
		
		Assert.assertEquals(initial+7,jdbcTemplate.queryForInt("select count(1) from product"));
	}
	
	@Test public void importProductsWithErrors() throws Exception {
		int initial = jdbcTemplate.queryForInt("select count(1) from product");
		
		jobLauncher.run(job, new JobParametersBuilder()
			.addString("inputResource", "classpath:/input/products_with_errors.zip")
			.addString("targetDirectory", "./target/importproductsbatch/")
			.addString("targetFile","products.txt")
			.addLong("timestamp", System.currentTimeMillis())
			.toJobParameters()
		);
		
		Assert.assertEquals(initial+6,jdbcTemplate.queryForInt("select count(1) from product"));
	}
	
	@Test public void missingParameters() throws Exception {		
		try {
			jobLauncher.run(job, new JobParametersBuilder()
				.addString("inputResource", "classpath:/input/products_with_errors.zip")
				.toJobParameters()
			);
			Assert.fail("missing parameters, the job should not have been launched");
		} catch (JobParametersInvalidException e) {
			// OK
		}		
	}
	
	@Test public void inputDoesNotExist() throws Exception {	
		try {
			jobLauncher.run(job, new JobParametersBuilder()
				.addString("inputResource", "classpath:/input/bad_products_input.zip")
				.addString("targetDirectory", "./target/importproductsbatch/")
				.addString("targetFile","products.txt")
				.toJobParameters()
			);
			Assert.fail("the input does not exist, the job should not have been launched");
		} catch (JobParametersInvalidException e) {
			// OK	
		}		
	}
	
}
