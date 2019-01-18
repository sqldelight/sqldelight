//
//  TeamsViewController.swift
//  iosApp
//
//  Created by Kevin Galligan on 1/18/19.
//  Copyright © 2019 Kevin Galligan. All rights reserved.
//

import Foundation
import UIKit
import common

class TeamsViewController: UIViewController, UITableViewDelegate, UITableViewDataSource {
    var teamData:TeamData? = nil
    
    @IBOutlet weak var tableView: UITableView!
    override func viewDidLoad() {
        super.viewDidLoad()
        
        tableView.delegate = self
        tableView.dataSource = self

        teamData = TeamData(updateNotifier: showTeams)
    }
    
    func showTeams() -> KotlinUnit{
        tableView.reloadData()
        return KotlinUnit()
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: false)
        
        let team = teamData?.findRow(index: Int32(indexPath.row))
        
        performSegue(withIdentifier: "ShowPlayers", sender: team)
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "ShowPlayers" {
            let detailViewController = segue.destination as! PlayersViewController
            let team = sender as! Team
            detailViewController.teamId = team.id
        }
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if(teamData == nil){
            return 0
        }
        else{
            return Int(teamData!.size)
        }
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "teamRow", for: indexPath) as! TeamRow
        teamData?.fillRow(index: Int32(indexPath.row), cell: cell)
        
        cell.layer.isOpaque = true
        
        return cell
    }
}
