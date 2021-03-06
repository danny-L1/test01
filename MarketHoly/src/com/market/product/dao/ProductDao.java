package com.market.product.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

import com.market.admin.dto.ProdInfoDto;
import com.market.db.JDBCUtil;
import com.market.product.dto.ProductDto;

public class ProductDao {

	public int getMaxNum() {
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			con = JDBCUtil.getConn();
			String sql = "select max(ifnull(pnum,0)) maxnum from product";
			pstmt = con.prepareStatement(sql);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getInt("maxnum");
			} else {
				return 0;
			}
		} catch (SQLException se) {
			System.out.println(se.getMessage());
			return -1;
		} finally {
			JDBCUtil.close(rs, pstmt, con);
		}

	}

	public int getCount(int cnum, int type, String keyword) {
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sql = null;
		try {
			con = JDBCUtil.getConn();

			if (type == 0 && cnum == 0) {
				sql = "select ifnull(count(pnum),0) cnt from product where 1=1";
				// 검색리스트일때
				if (keyword != "") {
					sql += " and name like ?";
					pstmt = con.prepareStatement(sql);
					pstmt.setString(1, "%" + keyword + "%");
				} else {
					sql += " order by reg_date desc";
					pstmt = con.prepareStatement(sql);
				}

			} else if (type == -1) {
				sql = "select ifnull(count(pnum),0) cnt from product where type=?";
				pstmt = con.prepareStatement(sql);
				pstmt.setInt(1, cnum);
			} else {
				sql = "select ifnull(count(pnum),0) cnt from product where cnum=? and type=? ";
				pstmt = con.prepareStatement(sql);
				pstmt.setInt(1, cnum);
				pstmt.setInt(2, type);
			}

			rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getInt("cnt");
			} else {
				return 0;
			}
		} catch (SQLException se) {
			System.out.println(se.getMessage());
			return -1;
		} finally {
			JDBCUtil.close(rs, pstmt, con);
		}

	}

	// 신상품/베스트/할인상품 카운트
	public int getNBSCount(String filter) {
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sql = null;
		try {
			con = JDBCUtil.getConn();
			sql = "select ifnull(count(p.pnum),0) cnt from product p";
			if (filter.equals("new")) {
				sql += " where reg_date between DATE_SUB(now(), interval 7 day) and now() and p.del_yn='N'";

			} else if (filter.equals("best")) {
				sql += ",order_product op where p.pnum=op.pnum group by p.pnum and p.del_yn='N'";

			} else if (filter.equals("sale")) {
				sql += ",sale s where p.pnum=s.pnum and p.del_yn='N'";
			}
			pstmt = con.prepareStatement(sql);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getInt("cnt");
			} else {
				return 0;
			}
		} catch (SQLException se) {
			System.out.println(se.getMessage());
			return -1;
		} finally {
			JDBCUtil.close(rs, pstmt, con);
		}
	}

	// 상세페이지
	public ProductDto getDetail(int pnum) {
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		ProductDto dto = null;
		try {
			con = JDBCUtil.getConn();
			String sql = " select *, ifnull(s.percent,1)percent \n" + 
					"from product p\n" + 
					"left outer join sale s on(p.pnum = s.pnum)\n" + 
					"where p.pnum=?";
			pstmt = con.prepareStatement(sql);
			pstmt.setInt(1, pnum);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				String name = rs.getString("name");
				Date reg_date = rs.getDate("reg_date");
				int price = rs.getInt("price");
				int stock = rs.getInt("stock");
				String thumb_org = rs.getString("thumb_org");
				String thumb_save = rs.getString("thumb_save");
				String detail_org = rs.getString("detail_org");
				String detail_save = rs.getString("detail_save");
				String description = rs.getString("description");
				String del_yn = rs.getString("del_yn");
				float percent = rs.getFloat("percent");
				dto = new ProductDto(pnum, 0, name, reg_date, price, stock, 0, thumb_org, thumb_save, description,
						detail_org, detail_save, del_yn, percent);
			}
			return dto;

		} catch (SQLException se) {
			System.out.println(se.getMessage());
			return null;
		} finally {
			JDBCUtil.close(rs, pstmt, con);
		}
	}

	public ArrayList<ProductDto> getList(int startRow, String list_filter, int cnum, int type) {
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sql = null;
		ArrayList<Integer> paramList = new ArrayList<Integer>();
		ArrayList<ProductDto> list = new ArrayList<ProductDto>();
		try {
			con = JDBCUtil.getConn();

			sql = "select p.*,ifnull(s.percent,1)percent \r\n" + 
					"from product p left outer join sale s on(p.pnum = s.pnum)\r\n" + 
					"where 1=1 "; 
			if (type == -1) { //대분류
				sql += "and type=?";
				paramList.add(cnum);
			} else if (cnum == 0 && type == 0) { //카테고리 x
				sql += "";
			} else {
				sql += "and cnum=? and type=?";
				paramList.add(cnum);
				paramList.add(type);
			}
			String sort;
			if (list_filter == null) {
				list_filter = "new";
			}

			switch (list_filter) {
			case "best":
				sort = "reg_date";
				break;
			case "lowprice":
				sort = "price";
				break;
			case "highprice":
				sort = "price desc";
				break;
			case "new":
			default:
				sort = "reg_date desc";
				break;
			}

			sql += " order by " + sort + " limit ?,6";

			paramList.add(startRow);
			pstmt = con.prepareStatement(sql);

			ListIterator<Integer> iter = paramList.listIterator();

			while (iter.hasNext()) {
				pstmt.setInt(iter.nextIndex() + 1, iter.next());
			}

			rs = pstmt.executeQuery();

			while (rs.next()) {
				int pnum = rs.getInt("pnum");
				String name = rs.getString("name");
				Date reg_date = rs.getDate("reg_date");
				int price = rs.getInt("price");
				int stock = rs.getInt("stock");
				String thumb_org = rs.getString("thumb_org");
				String thumb_save = rs.getString("thumb_save");
				String description = rs.getString("description");
				String del_yn = rs.getString("del_yn");
				float percent = rs.getFloat("percent");
				list.add(new ProductDto(pnum, cnum, name, reg_date, price, stock, type, thumb_org, thumb_save,
						description, null, null, del_yn,percent));

			}
			return list;

		} catch (SQLException se) {
			System.out.println(se.getMessage());
			return null;
		} finally {
			JDBCUtil.close(rs, pstmt, con);
		}
	}

	// 신상품,베스트,세일상품 리스트
	public ArrayList<ProductDto> getNBSList(int startRow, String filter) {
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sql = "";
		ArrayList<ProductDto> list = new ArrayList<ProductDto>();
		try {
			con = JDBCUtil.getConn();
			if (filter.equals("new")) {
				sql = "select p.*,ifnull(s.percent,1)percent  from product p\n"
						+ "left outer join sale s on p.pnum = s.pnum  \n"
						+ "where p.reg_date between DATE_SUB(NOW(), INTERVAL 7 DAY) and NOW() and P.del_yn='N'\n"
						+ "order by reg_date desc "+
						"limit ?,6";
				pstmt = con.prepareStatement(sql);
				pstmt.setInt(1, startRow);
				rs = pstmt.executeQuery();

				while (rs.next()) {
					int pnum = rs.getInt("pnum");
					String name = rs.getString("name");
					Date reg_date = rs.getDate("reg_date");
					int price = rs.getInt("price");
					int stock = rs.getInt("stock");
					String thumb_save = rs.getString("thumb_save");
					String description = rs.getString("description");
					float percent=rs.getFloat("percent");
					list.add(
							new ProductDto(pnum, name, reg_date, price, stock, thumb_save, description, percent));
				}
			}else if(filter.equals("best")) {
				sql = "select aa.*,ifnull(s.percent,1)percent\n" + 
						"from\n" + 
						"(select b.pnum, name pname ,b.reg_date,b.price,b.stock,b.thumb_save,b.description,b.del_yn, ifnull(count(a.pnum),0)cnt\n" + 
						"from order_product as a\n" + 
						"inner join product as b\n" + 
						"on a.pnum = b.pnum\n" + 
						"where del_yn='N'\n" + 
						"group by a.pnum\n" + 
						"order by cnt desc)aa, sale s "
						+ "where s.pnum =aa.pnum group by aa.pnum "+
						"limit ?,6";
				pstmt = con.prepareStatement(sql);
				pstmt.setInt(1, startRow);
				rs = pstmt.executeQuery();

				while (rs.next()) {
					int pnum = rs.getInt("pnum");
					String name = rs.getString("pname");
					Date reg_date = rs.getDate("reg_date");
					int price = rs.getInt("price");
					int stock = rs.getInt("stock");
					String thumb_save = rs.getString("thumb_save");
					String description = rs.getString("description");
					float percent=rs.getFloat("percent");
					list.add(
							new ProductDto(pnum, name, reg_date, price, stock, thumb_save, description, percent));
				
				}
			} else if (filter.equals("sale")) {
				sql = "select p.pnum,p.name pname,p.reg_date,p.price,p.stock,p.thumb_save,\r\n" + 
						"p.description,s.percent \r\n" + 
						"from product p,sale s where p.pnum=s.pnum and p.del_yn='N'\r\n" + 
						"order by p.reg_date desc\r\n" + 
						" limit ?,6";
				pstmt = con.prepareStatement(sql);
				pstmt.setInt(1, startRow);

				rs = pstmt.executeQuery();

				while (rs.next()) {
					int pnum = rs.getInt("pnum");
					String name = rs.getString("pname");
					Date reg_date = rs.getDate("reg_date");
					int price = rs.getInt("price");
					int stock = rs.getInt("stock");
					String thumb_save = rs.getString("thumb_save");
					String description = rs.getString("description");
					float percent=rs.getFloat("percent");
					list.add(
							new ProductDto(pnum, name, reg_date, price, stock, thumb_save, description, percent));
				}
			}
			return list;

		} catch (SQLException se) {
			System.out.println(se.getMessage());
			return null;
		} finally {
			JDBCUtil.close(rs, pstmt, con);
		}
	}

	// 검색 리스트
	public ArrayList<ProductDto> getSearchList(int startRow, String keyword) {
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		ArrayList<ProductDto> list = new ArrayList<ProductDto>();
		try {
			con = JDBCUtil.getConn();

			String sql = "select p.*,ifnull(s.percent,1)percent from product p \r\n" + 
					"left outer join sale s on(p.pnum = s.pnum)\r\n" + 
					"where p.name like ? and p.del_yn='N'\r\n" + 
					"limit ?,6";
			pstmt = con.prepareStatement(sql);
			pstmt.setString(1, "%" + keyword + "%");
			pstmt.setInt(2, startRow);
			rs = pstmt.executeQuery();

			while (rs.next()) {
				int pnum = rs.getInt("pnum");
				String name = rs.getString("name");
				Date reg_date = rs.getDate("reg_date");
				int price = rs.getInt("price");
				int stock = rs.getInt("stock");
				String thumb_save = rs.getString("thumb_save");
				String description = rs.getString("description");
				float percent=rs.getFloat("percent");
				list.add(
						new ProductDto(pnum,name,reg_date,price,stock,thumb_save,description,percent));

			}
			return list;

		} catch (SQLException se) {
			System.out.println(se.getMessage());
			return null;
		} finally {
			JDBCUtil.close(rs, pstmt, con);
		}
	}

	public int delProd(int pnum) {
		Connection con = null;
		PreparedStatement pstmt = null;
		PreparedStatement pstmt1 = null;
		PreparedStatement pstmt2 = null;
		PreparedStatement pstmt3 = null;
		PreparedStatement pstmt4 = null;
		PreparedStatement pstmt5 = null;
		PreparedStatement pstmt6 = null;
		try {
			con = JDBCUtil.getConn();
			con.setAutoCommit(false);
		
			String sql6 = "delete from review where pnum = ?";
			pstmt6 = con.prepareStatement(sql6);
			pstmt6.setInt(1, pnum);
			pstmt6.executeUpdate();
			
			String sql5 = "delete from prod_info where pnum = ?";
			pstmt5 = con.prepareStatement(sql5);
			pstmt5.setInt(1, pnum);
			pstmt5.executeUpdate();
			
			String sql4 = "delete from cart where pnum = ?";
			pstmt4 = con.prepareStatement(sql4);
			pstmt4.setInt(1, pnum);
			pstmt4.executeUpdate();
			
			String sql3 = "delete from order_product where pnum = ?";
			pstmt3 = con.prepareStatement(sql3);
			pstmt3.setInt(1, pnum);
			pstmt3.executeUpdate();
			
			String sql2 = "delete from sale where pnum = ?";
			pstmt2 = con.prepareStatement(sql2);
			pstmt2.setInt(1, pnum);
			pstmt2.executeUpdate();
			
			String sql1 = "delete from qna where pnum = ?";
			pstmt1 = con.prepareStatement(sql1);
			pstmt1.setInt(1, pnum);
			pstmt1.executeUpdate();
			
			String sql = "delete from product where pnum = ?";
			pstmt = con.prepareStatement(sql);
			pstmt.setInt(1, pnum);
			int n = pstmt.executeUpdate();
			
			return n;
		} catch (SQLException se) {
			System.out.println(se.getMessage());
			try {
				con.rollback();
			} catch (SQLException e) {
				System.out.println(e.getMessage());
			}
			return -1;
		} finally {
			try {
				con.commit();
				con.setAutoCommit(true);
			} catch (SQLException e) {
				System.out.println(e.getMessage());
			}
			JDBCUtil.close(null, pstmt, con);
		}
	}

	public ProdInfoDto getProdInfo(int pnum) {
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			con = JDBCUtil.getConn();
			String sql = "select * from prod_info where pnum = ?";
			pstmt = con.prepareStatement(sql);
			pstmt.setInt(1, pnum);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				String unit = rs.getString("unit");
				String volume = rs.getString("volume");
				String origin = rs.getString("origin");
				String pack_type = rs.getString("pack_type");
				String shelf_life = rs.getString("shelf_life");
				String guidance = rs.getString("guidance");
				return new ProdInfoDto(pnum, unit, volume, origin, pack_type, shelf_life, guidance);
			}
			return null;
		} catch (SQLException se) {
			System.out.println(se.getMessage());
			return null;
		} finally {
			JDBCUtil.close(rs, pstmt, con);
		}
	}

}